package examples

import chisel3._
import chisel3.stage._
import chisel3.util._
import firrtl.PrimOps
import scala.math._
import scala.reflect.runtime.Macros
import javax.swing.plaf.basic.BasicToolBarUI


//想参数化，但是貌似不是很好参数化的样子，但是还好，直接粘贴过去改一下就行
class  fifo  (length :Int,width :Int,write_num:Int,read_num:Int) extends Module  with mips_macros{
    val bank_width = log2Up(write_num.max(read_num))
    val bank_num  = pow(2,bank_width).toInt
    val length_width = (log10(length)/log10(2)).toInt
    val io = IO(new Bundle { 
        val read_en = Input(UInt(bank_width.W))
        val write_en = Input(UInt(bank_width.W))//0为前面的
        val read_out  = Vec(read_num,Output(UInt(width.W)))//0为前面的
        val write_in  = Vec(write_num,Input(UInt(width.W)))//0为前面的
        val full = Output(Bool()) //浪费一点空间无所谓，只要剩余的空间小于最大的写入空间，就算满
        val empty = Output(Bool())
        val read_length_point = Output(UInt(length_width.W))
        val read_bank_point = Output(UInt(bank_width.W))
        // val read_length_point_write = Input(UInt(length_width.W))
        // val read_bank_point_write   = Input(UInt(bank_width.W))
        val point_write_en = Input(Bool())
        val point_flush = Input(Bool()) //清空整个指令序列
        // val empty = Output(Bool()) //只有满足超过发射大小的情况下才叫做不空 ,不需要empty判定吧，我这个算是写优先】的效果
        //感觉应该还算比较难满的把 感觉
    })
    def value2valid(value:UInt,width:Int)  :UInt= {
        MuxLookup(value,0.U(width.W),Seq(
            0.U -> 0.U,
            1.U -> "b001".U,
            2.U -> "b011".U,
            3.U -> "b111".U
        ))
    }

    val fifo_banks = VecInit(Seq.fill(bank_num)(Module(new Look_up_table(length,width)).io))
    val write_banks_points = RegInit(0.U(bank_width.W))
    val write_length_points = RegInit(0.U(length_width.W))
    val read_banks_points = RegInit(0.U(bank_width.W))
    val read_length_points = RegInit(0.U(length_width.W))

    val write_valid = value2valid(io.write_en,3)
    for(i <- 0 until bank_num) {
        fifo_banks(i.asUInt).aw_addr := MuxLookup(i.asUInt,0.U,Seq(
            write_banks_points -> write_length_points,
            write_banks_points + 1.U -> Mux(Cat(0.U(1.W),write_banks_points) + 1.U < bank_num.asUInt,write_length_points,write_length_points + 1.U),
            write_banks_points + 2.U -> Mux(Cat(0.U(1.W),write_banks_points) + 2.U < bank_num.asUInt,write_length_points,write_length_points + 1.U)
        ))
        fifo_banks(i.asUInt).ar_addr := MuxLookup(i.asUInt,0.U,Seq(
            read_banks_points -> read_length_points,
            read_banks_points + 1.U -> Mux(Cat(0.U(1.W),read_banks_points) + 1.U < bank_num.asUInt,read_length_points,read_length_points + 1.U),
            read_banks_points + 2.U -> Mux(Cat(0.U(1.W),read_banks_points) + 2.U < bank_num.asUInt,read_length_points,read_length_points + 1.U)
        ))
        fifo_banks(i.asUInt).in := MuxLookup(i.asUInt,0.U,Seq(
            write_banks_points -> io.write_in(0),
            write_banks_points + 1.U -> io.write_in(1),
            write_banks_points + 2.U -> io.write_in(2)
        ))
        fifo_banks(i.asUInt).write := MuxLookup(i.asUInt,0.U,Seq(
            write_banks_points -> write_valid(0),
            write_banks_points + 1.U -> write_valid(1),
            write_banks_points + 2.U -> write_valid(2)
        ))


        // fifo_banks(write_banks_points + i.asUInt).aw_addr :=    (write_length_points   + Mux((Cat(0.U(1.W),write_banks_points) + i.asUInt)(bank_width),1.U,0.U))(length_width - 1,0)
        // fifo_banks(write_banks_points + i.asUInt).ar_addr :=    (read_length_points +  Mux((Cat(0.U(1.W),read_banks_points) + i.asUInt)(bank_width),1.U,0.U))(length_width - 1,0)
        // fifo_banks(write_banks_points + i.asUInt).write   :=    i.asUInt < io.write_en
        // fifo_banks(write_banks_points + i.asUInt).in      :=    io.write_in(i)
    }
    // val branch_error_state = RegInit(0.U(1.W))
    // val branch_error_has_finished = Wire(Bool())
    //val branch_error_disable = RegInit(Bool())
    //val point_write_en_state = RegInit(Bool())
    //point_write_en_state := Mux(io.point_write_en,0.U,Mux(io.read_en =/= 0.U ,0.U,point_write_en_state) )//&& branch_error_has_finished

    // branch_error_disable := 
   // branch_error_state := Mux(io.point_write_en.asBool && io.empty.asBool,1.U,Mux(branch_error_has_finished,0.U,branch_error_state))
   //================must have error
    val point_write_tag = Mux(io.point_write_en && (!io.empty ||(io.empty.asBool && io.write_en =/= 0.U)),1.U,0.U)

  //  branch_error_has_finished := point_write_tag.asBool
    // 因为分支指令的处理就在id阶段，所以中间并不会向流水线中添加指令

    //===========================
    write_banks_points := Mux(io.point_flush,0.U,Mux(point_write_tag.asBool,read_banks_points + 1.U,(write_banks_points + io.write_en)(bank_width - 1,0)))
//假如出现需要跳转的分支指令的时候我需要做的事情，read_point 暂时不变 write point移动到分支延迟槽的位置
    // write_length_points := (write_length_points + Mux(point_write_tag.asBool,write_banks_points === 3.U,
    //     Mux((Cat(0.U(1.W),write_banks_points) + io.write_en)(bank_width),1.U,0.U)))(length_width - 1,0)
    write_length_points := Mux(io.point_flush,0.U,Mux(point_write_tag.asBool,read_length_points + Mux(read_banks_points === 3.U,1.U,0.U),(write_length_points +  Mux((Cat(0.U(1.W),write_banks_points) + io.write_en)(bank_width),1.U,0.U))(length_width - 1,0)))
    read_banks_points := Mux(io.point_flush,0.U,(read_banks_points + io.read_en)(bank_width - 1,0))
    read_length_points := Mux(io.point_flush,0.U, (read_length_points + Mux((Cat(0.U(1.W),read_banks_points) + io.read_en)(bank_width),1.U,0.U))(length_width - 1,0))
    
    // io.full := Mux(write_length_points === read_length_points,write_banks_points >= read_banks_points || write_banks_points <= read_banks_points - write_num.asUInt,
    //     Mux(write_length_points === read_length_points - 1.U,bank_num.asUInt - write_banks_points + read_banks_points <= write_num.asUInt ,0.U.asBool))
    
    for(i <- 0 until read_num) {
        io.read_out(i.asUInt) := Mux(io.empty,0.U,fifo_banks(read_banks_points + i.asUInt).out)
    }


    io.empty := write_banks_points === read_banks_points && write_length_points === read_length_points
    // write 和 read 在一排肯定满了
    io.full  := Mux(write_length_points === read_length_points, write_banks_points < read_banks_points,
    Mux(write_length_points + 1.U === read_length_points ,
        (Cat(0.U(1.W),write_banks_points) + 3.U) >=  (Cat(0.U(1.W),read_banks_points) + 4.U) ,0.U))
    io.read_bank_point := read_banks_points
    io.read_length_point := read_length_points
}                                              

object fifo_test extends App{
    (new ChiselStage).emitVerilog(new fifo(128,32,3,2))
}


