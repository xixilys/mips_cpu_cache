package examples

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._
import scala.math._
import scala.reflect.runtime.Macros
import javax.swing.plaf.basic.BasicToolBarUI
class Look_up_table(length : Int,width : Int) extends Module  with mips_macros{
//类似于cache，但是不存在替换算法
    val addr_width = (log10(length)/log10(2)).toInt
    val io = IO(new Bundle {
        val ar_addr  = Input(UInt(addr_width.W))
        val aw_addr  = Input(UInt(addr_width.W))
        val write = Input(Bool()) // 0 => 不写入 01 => 部分写入 10 => 全写入
        val in = Input(UInt(width.W))
        val out = Output(UInt(width.W))
    })
    val btb = RegInit(VecInit(Seq.fill(length)(0.U(width.W))))
    io.out := Mux(io.write && io.aw_addr === io.ar_addr,io.in,btb(io.ar_addr))
    btb(io.aw_addr) := Mux(io.write,io.in, btb(io.aw_addr))
    // for(  i <- 0 to length - 1) {
    //     btb(i) := Mux(io.write && i.asUInt === io.aw_addr,io.in,btb(i))
    // }
}
class two_port_lookup_table_with_two_port (length :Int,width :Int)  extends Module {  //true two ports lookup table
    val length_width = (log10(length)/log10(2)).toInt
    val io = IO(new Bundle { 
        val write_addr = Vec(2,Input(UInt(length_width.W)))
        val read_addr = Vec(2,Input(UInt(length_width.W)))       
        val write_en = Input(UInt(2.W))//0为前面的
        val read_out  = Vec(2,Output(UInt(width.W)))//0为前面的
        val write_in  = Vec(2,Input(UInt(width.W)))//0为前面的
    }) //二读二写的lookup_table
    val table = RegInit(VecInit(Seq.fill(length)(0.U(width.W))))
    io.read_out(0) := Mux(io.write_en(0) && io.write_addr(0) === io.read_addr(0) , io.write_in(0) ,
        Mux(io.write_en(1) && io.write_addr(1) === io.read_addr(0) ,io.write_in(1), table(io.read_addr(0))))
    io.read_out(1) := Mux(io.write_en(0) && io.write_addr(0) === io.read_addr(1) , io.write_in(0) ,
        Mux(io.write_en(1) && io.write_addr(1) === io.read_addr(1) ,io.write_in(1), table(io.read_addr(1))))
    for(  i <- 0 to length - 1) {
        table(i) := Mux(io.write_en(0) && i.asUInt === io.write_addr(0),io.write_in(0),
            Mux(io.write_en(1) && i.asUInt === io.write_addr(1),io.write_in(1), table(i)))
    }
}

class two_ports_lookup_table (length :Int,width :Int)  extends Module {  //true two ports lookup table
    val length_width = (log10(length)/log10(2)).toInt
    val io = IO(new Bundle { 
        val write_addr = Vec(2,Input(UInt(length_width.W)))
        val read_addr = Vec(4,Input(UInt(length_width.W)))       
        val write_en = Input(UInt(2.W))//0为前面的
        val read_out  = Vec(4,Output(UInt(width.W)))//0为前面的
        val write_in  = Vec(2,Input(UInt(width.W)))//0为前面的
    }) //将两个二读二写的lookup_table转换成一个四读二写的lookup_table
    //不好做多bank，不方便
    val double_table = VecInit(Seq.fill(2)(Module(new two_port_lookup_table_with_two_port(length,width)).io))
    for(  i <- 0 to 1) {
        double_table(i).write_en := io.write_en
        double_table(i).write_addr := io.write_addr
        double_table(i).write_in := io.write_in
        double_table(i).read_addr(0) := io.read_addr(i * 2)
        double_table(i).read_addr(1) := io.read_addr(i * 2 + 1)
        io.read_out(i * 2)      := double_table(i).read_out(0)
        io.read_out(i * 2 + 1)  := double_table(i).read_out(1)
    }
}
// object two_ports_lookup_table_test extends App{
//     (new ChiselStage).emitVerilog(new two_ports_lookup_table(32,6) )
// }

