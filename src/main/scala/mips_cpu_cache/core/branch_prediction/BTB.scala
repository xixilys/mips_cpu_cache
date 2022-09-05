package examples

import chisel3._
import chisel3.stage._
import chisel3.util._
import firrtl.PrimOps
import scala.math._
import scala.reflect.runtime.Macros




class BTB_banks(length : Int,bank_num: Int) extends Module  with mips_macros{
//类似于cache，但是不存在替换算法
    val addr_width = (log10(length)/log10(2)).toInt
    val bank_num_width = (log10(bank_num)/log10(2)).toInt
    val io = IO(new Bundle { //分支指令不支持同时写
        val ar_addr_L  = Input(UInt(32.W))
        val ar_addr_M = Input(UInt(32.W))
        val ar_addr_R = Input(UInt(32.W))
        val aw_addr  = Input(UInt(32.W))
        val aw_target_addr = Input(UInt(32.W))
        val write = Input(Bool()) 
        val out_L = Output(UInt(32.W))
        val out_M = Output(UInt(32.W))
        val out_R = Output(UInt(32.W))
        val hit_L = Output(Bool())
        val hit_M = Output(Bool())
        val hit_R = Output(Bool())
    })
    //选择一直都是存width宽度的数据，尽量降低PHTS的复杂度ar_pht_addr
    //还没确定后面分支恢复的时候的时序，先按照不停流水线来做吧
    val tag_banks = VecInit(Seq.fill(bank_num)(Module(new Look_up_table(length,21 - (bank_num_width  + addr_width + 2))).io))
    val btb_banks = VecInit(Seq.fill(bank_num)(Module(new Look_up_table(length,32)).io))
    for(i <- 0 until bank_num ) {
        btb_banks(i).write := io.aw_addr(bank_num_width+1,2) === i.asUInt &&  io.write
        btb_banks(i).in := io.aw_target_addr
        btb_banks(i).ar_addr := MuxLookup(i.asUInt,0.U,Seq(
            io.ar_addr_L(bank_num_width + 1,2) -> io.ar_addr_L(addr_width + 3,4),       
            io.ar_addr_M(bank_num_width + 1,2) -> io.ar_addr_M(addr_width + 3,4),
            io.ar_addr_R(bank_num_width + 1,2) -> io.ar_addr_R(addr_width + 3,4)
        ))
        btb_banks(i).aw_addr := io.aw_addr
    }
    io.out_L := btb_banks(io.ar_addr_L(bank_num_width + 1,2) ).out
    io.out_M := btb_banks(io.ar_addr_M(bank_num_width + 1,2) ).out
    io.out_R := btb_banks(io.ar_addr_R(bank_num_width + 1,2) ).out

    for(i <- 0 until bank_num ) {
        tag_banks(i).write := io.aw_addr(bank_num_width+1,2) === i.asUInt &&  io.write
        tag_banks(i).in := io.aw_addr(20,bank_num_width + addr_width + 2)
        tag_banks(i).ar_addr := MuxLookup(i.asUInt,0.U,Seq(
            io.ar_addr_L(bank_num_width + 1,2) -> io.ar_addr_L(addr_width + 3,4),       
            io.ar_addr_M(bank_num_width + 1,2) -> io.ar_addr_M(addr_width + 3,4),
            io.ar_addr_R(bank_num_width + 1,2) -> io.ar_addr_R(addr_width + 3,4)
        ))
        tag_banks(i).aw_addr := io.aw_addr(bank_num_width + 1 + addr_width,2 + bank_num_width)
    }

    io.out_L := btb_banks(io.ar_addr_L(bank_num_width+1,2)).out
    io.out_M := btb_banks(io.ar_addr_M(bank_num_width+1,2)).out
    io.out_R := btb_banks(io.ar_addr_R(bank_num_width+1,2)).out
    io.hit_L := tag_banks(io.ar_addr_L(bank_num_width + 1,2)).out === io.ar_addr_L(21 - (bank_num_width  + addr_width + 2))
    io.hit_M := tag_banks(io.ar_addr_M(bank_num_width + 1,2)).out === io.ar_addr_M(21 - (bank_num_width  + addr_width + 2))
    io.hit_R := tag_banks(io.ar_addr_R(bank_num_width + 1,2)).out === io.ar_addr_R(21 - (bank_num_width  + addr_width + 2))
}
// object BTBS_banks_test extends App{
//     (new ChiselStage).emitVerilog(new BTB_banks(128,4))
// }

class BTB_banks_oneissue(length : Int,bank_num: Int) extends Module  with mips_macros{
//类似于cache，但是不存在替换算法
    val addr_width = (log10(length)/log10(2)).toInt
    val bank_num_width = (log10(bank_num)/log10(2)).toInt
    val io = IO(new Bundle { //分支指令不支持同时写
        val ar_addr_L  = Input(UInt(32.W))
        val ar_addr_M = Input(UInt(32.W))
        val ar_addr_R = Input(UInt(32.W))
        val aw_addr  = Input(UInt(32.W))
        val aw_target_addr = Input(UInt(32.W))
        val write = Input(Bool()) 
        val out_L = Output(UInt(32.W))
        val out_M = Output(UInt(32.W))
        val out_R = Output(UInt(32.W))
        val hit_L = Output(Bool())
        val hit_M = Output(Bool())
        val hit_R = Output(Bool())
    })
    //选择一直都是存width宽度的数据，尽量降低PHTS的复杂度ar_pht_addr
    //还没确定后面分支恢复的时候的时序，先按照不停流水线来做吧
    val tag_banks = VecInit(Seq.fill(bank_num)(Module(new Look_up_table_read_first_(length,1 + 17 - (bank_num_width  + addr_width + 2))).io))
    val btb_banks = VecInit(Seq.fill(bank_num)(Module(new Look_up_table_read_first_(length,32)).io))
    for(i <- 0 until bank_num ) {
        btb_banks(i).write := io.aw_addr(bank_num_width + 1,2) === i.asUInt &&  io.write
        btb_banks(i).in := io.aw_target_addr
        btb_banks(i).ar_addr := io.ar_addr_L(addr_width + 3,4)
        // MuxLookup(i.asUInt,0.U,Seq(
        // io.ar_addr_L(bank_num_width + 1,2) -> io.ar_addr_L(addr_width + 3,4),       
        // io.ar_addr_M(bank_num_width + 1,2) -> io.ar_addr_M(addr_width + 3,4),
        // io.ar_addr_R(bank_num_width + 1,2) -> io.ar_addr_R(addr_width + 3,4)
        // ))
        btb_banks(i).aw_addr := io.aw_addr(addr_width + 3,4)
    }
    io.out_L := btb_banks(io.ar_addr_L(bank_num_width + 1,2) ).out
    io.out_M := btb_banks(io.ar_addr_M(bank_num_width + 1,2) ).out
    io.out_R := btb_banks(io.ar_addr_R(bank_num_width + 1,2) ).out

    for(i <- 0 until bank_num ) {
        tag_banks(i).write := io.aw_addr(bank_num_width + 1,2) === i.asUInt &&  io.write
        tag_banks(i).in := Cat(1.U(1.W),io.aw_addr(16,bank_num_width + addr_width + 2))
        tag_banks(i).ar_addr := io.ar_addr_L(addr_width + 3,4)
        //     MuxLookup(i.asUInt,0.U,Seq(
        //     io.ar_addr_L(bank_num_width + 1,2) -> io.ar_addr_L(addr_width + 3,4),       
        //     io.ar_addr_M(bank_num_width + 1,2) -> io.ar_addr_M(addr_width + 3,4),
        //     io.ar_addr_R(bank_num_width + 1,2) -> io.ar_addr_R(addr_width + 3,4)
        // ))
        tag_banks(i).aw_addr := io.aw_addr(addr_width + 3,4)
    }

    io.out_L := btb_banks(io.ar_addr_L(bank_num_width + 1,2)).out
    io.out_M := btb_banks(io.ar_addr_M(bank_num_width + 1,2)).out
    io.out_R := btb_banks(io.ar_addr_R(bank_num_width + 1,2)).out
    //为什么是16呢，因为官方测试程序里面最大位宽就是第17位
    io.hit_L := tag_banks(io.ar_addr_L(bank_num_width + 1,2)).out(16 - bank_num_width - addr_width - 2 ,0) === io.ar_addr_L(16 , (bank_num_width  + addr_width + 2)) && tag_banks(io.ar_addr_L(bank_num_width + 1 ,2)).out(16 - bank_num_width - addr_width - 2 + 1) 
    io.hit_M := tag_banks(io.ar_addr_M(bank_num_width + 1,2)).out(16 - bank_num_width - addr_width - 2 ,0) === io.ar_addr_M(16 , (bank_num_width  + addr_width + 2)) && tag_banks(io.ar_addr_M(bank_num_width + 1 ,2)).out(16 - bank_num_width - addr_width - 2 + 1)
    io.hit_R := tag_banks(io.ar_addr_R(bank_num_width + 1,2)).out(16 - bank_num_width - addr_width - 2 ,0) === io.ar_addr_R(16 , (bank_num_width  + addr_width + 2)) && tag_banks(io.ar_addr_R(bank_num_width + 1 ,2)).out(16 - bank_num_width - addr_width - 2 + 1)
}
// object BTBS_banks_test extends App{
//     (new ChiselStage).emitVerilog(new BTB_banks(128,4))
// }

//  input clka;
//   input ena;
//   input [0:0]wea;
//   input [8:0]addra;
//   input [31:0]dina;
//   input clkb;
//   input enb;
//   input [8:0]addrb;
//   output [31:0]doutb;
class btb_data_ram(length : Int) extends BlackBox {
     val addr_width = (log10(length)/log10(2)).toInt
    val io = IO(new Bundle {
     
    val        clka = Input(UInt(1.W))
    val        clkb = Input(UInt(1.W))
    val        ena  = Input(UInt(1.W))
    val        enb  = Input(UInt(1.W))
    val        wea   = Input(UInt(1.W)) //没有使能对于字的写
    //a端口为读 端口为写
    val        addra   = Input(UInt(addr_width.W))
    val        dina  = Input(UInt(32.W))
    val        addrb   = Input(UInt(addr_width.W))
    val        doutb  = Output(UInt(32.W))
  
    })
}
class btb_tag_ram(length : Int) extends BlackBox {
     val addr_width = (log10(length)/log10(2)).toInt
    val io = IO(new Bundle {
     
    val        clka = Input(UInt(1.W))
    val        clkb = Input(UInt(1.W))
    val        ena  = Input(UInt(1.W))
    val        enb  = Input(UInt(1.W))
    val        wea   = Input(UInt(1.W)) //没有使能对于字的写
    //a端口为读 端口为写
    val        addra   = Input(UInt(addr_width.W))
    val        dina  = Input(UInt(8.W))
    val        addrb   = Input(UInt(addr_width.W))
    val        doutb  = Output(UInt(8.W))
  
    })
}
class btb_data_with_block_ram(length : Int)  extends Module with mips_macros {
        //完全没用到chisel真正好的地方，我是废物呜呜呜呜
         val addr_width = (log10(length)/log10(2)).toInt
    val io = IO(new Bundle {
     
    val        en   = Input(UInt(1.W))
    val        wen   = Input(UInt(1.W))
    val        raddr   = Input(UInt(addr_width.W))
    val        waddr   = Input(UInt(addr_width.W))
    val        wdata   = Input(UInt(32.W))
    val        rdata  = Output(UInt(32.W))
  
    })
    //a通道为写 b通道为读
    val btb_data_ram_0 = Module(new btb_data_ram(512))
    btb_data_ram_0.io.clka := clock.asUInt
    btb_data_ram_0.io.clkb := clock.asUInt
    btb_data_ram_0.io.ena   := io.en
    btb_data_ram_0.io.enb   := io.en
    btb_data_ram_0.io.wea  := io.wen
    btb_data_ram_0.io.addra := io.waddr
    btb_data_ram_0.io.addrb := io.raddr
    btb_data_ram_0.io.dina := io.wdata
    io.rdata     := btb_data_ram_0.io.doutb
}


class btb_tag_with_block_ram(length : Int)  extends Module with mips_macros {
        //完全没用到chisel真正好的地方，我是废物呜呜呜呜
         val addr_width = (log10(length)/log10(2)).toInt
    val io = IO(new Bundle {
     
    val        en   = Input(UInt(1.W))
    val        wen   = Input(UInt(1.W))
    val        raddr   = Input(UInt(addr_width.W))
    val        waddr   = Input(UInt(addr_width.W))
    val        wdata   = Input(UInt(8.W))
    val        rdata  = Output(UInt(8.W))
  
    })
    //a通道为写 b通道为读
    val btb_tag_ram_0 = Module(new btb_tag_ram(512))
    btb_tag_ram_0.io.clka := clock.asUInt
    btb_tag_ram_0.io.clkb := clock.asUInt
    btb_tag_ram_0.io.ena   := io.en
    btb_tag_ram_0.io.enb   := io.en
    btb_tag_ram_0.io.wea  := io.wen
    btb_tag_ram_0.io.addra := io.waddr
    btb_tag_ram_0.io.addrb := io.raddr
    btb_tag_ram_0.io.dina := io.wdata
    io.rdata     := btb_tag_ram_0.io.doutb
}

class BTB_banks_oneissue_with_block_ram(length : Int,bank_num: Int) extends Module  with mips_macros{
//类似于cache，但是不存在替换算法
    val addr_width = (log10(length)/log10(2)).toInt
    val bank_num_width = (log10(bank_num)/log10(2)).toInt
    val io = IO(new Bundle { //分支指令不支持同时写
        val ar_addr_L  = Input(UInt(32.W))
        val ar_addr_M = Input(UInt(32.W))
        val ar_addr_R = Input(UInt(32.W))
        val aw_addr  = Input(UInt(32.W))
        val aw_target_addr = Input(UInt(32.W))
        val write = Input(Bool()) 
        val out_L = Output(UInt(32.W))
        val out_M = Output(UInt(32.W))
        val out_R = Output(UInt(32.W))
        val hit_L = Output(Bool())
        val hit_M = Output(Bool())
        val hit_R = Output(Bool())
    })
    //选择一直都是存width宽度的数据，尽量降低PHTS的复杂度ar_pht_addr
    //还没确定后面分支恢复的时候的时序，先按照不停流水线来做吧
    //val tag_banks = VecInit(Seq.fill(bank_num)(Module(new btb_tag_with_block_ram(length/*,1 + 17 - (bank_num_width  + addr_width + 2))*/).io))
    val tag_banks = VecInit(Seq.fill(bank_num)(Module(new btb_tag_with_block_ram(length)).io))
    val btb_banks = VecInit(Seq.fill(bank_num)(Module(new btb_data_with_block_ram(length)).io))
    for(i <- 0 until bank_num ) {
        btb_banks(i).en := 1.U
        btb_banks(i).wen := io.aw_addr(bank_num_width + 1,2) === i.asUInt &&  io.write
        btb_banks(i).wdata := io.aw_target_addr
        btb_banks(i).raddr := io.ar_addr_L(addr_width + 3,4)
        // MuxLookup(i.asUInt,0.U,Seq(
        // io.ar_addr_L(bank_num_width + 1,2) -> io.ar_addr_L(addr_width + 3,4),       
        // io.ar_addr_M(bank_num_width + 1,2) -> io.ar_addr_M(addr_width + 3,4),
        // io.ar_addr_R(bank_num_width + 1,2) -> io.ar_addr_R(addr_width + 3,4)
        // ))
        btb_banks(i).waddr := io.aw_addr(addr_width + 3,4)
    }
    val ar_addr_reg = RegInit(0.U(32.W))
    ar_addr_reg :=  io.ar_addr_L
    io.out_L := btb_banks(ar_addr_reg(bank_num_width + 1,2) ).rdata
    io.out_M := btb_banks(ar_addr_reg(bank_num_width + 1,2) ).rdata
    io.out_R := btb_banks(ar_addr_reg(bank_num_width + 1,2) ).rdata

    for(i <- 0 until bank_num ) {
        tag_banks(i).en := 1.U
        tag_banks(i).wen := io.aw_addr(bank_num_width + 1,2) === i.asUInt &&  io.write
        tag_banks(i).wdata := Cat(1.U(1.W),io.aw_addr(16,bank_num_width + addr_width + 2))
        tag_banks(i).raddr := io.ar_addr_L(addr_width + 3,4)
        //     MuxLookup(i.asUInt,0.U,Seq(
        //     io.ar_addr_L(bank_num_width + 1,2) -> io.ar_addr_L(addr_width + 3,4),       
        //     io.ar_addr_M(bank_num_width + 1,2) -> io.ar_addr_M(addr_width + 3,4),
        //     io.ar_addr_R(bank_num_width + 1,2) -> io.ar_addr_R(addr_width + 3,4)
        // ))
        tag_banks(i).waddr := io.aw_addr(addr_width + 3,4)
    }

    // io.out_L := btb_banks(io.ar_addr_L(bank_num_width + 1,2)).rdata
    // io.out_M := btb_banks(io.ar_addr_M(bank_num_width + 1,2)).rdata
    // io.out_R := btb_banks(io.ar_addr_R(bank_num_width + 1,2)).rdata
    //为什么是16呢，因为官方测试程序里面最大位宽就是第17位
    io.hit_L := tag_banks(ar_addr_reg(bank_num_width + 1,2)).rdata(16 - bank_num_width - addr_width - 2 ,0) === io.ar_addr_L(16 , (bank_num_width  + addr_width + 2)) && tag_banks(ar_addr_reg(bank_num_width + 1 ,2)).rdata(16 - bank_num_width - addr_width - 2 + 1) 
    io.hit_M := tag_banks(ar_addr_reg(bank_num_width + 1,2)).rdata(16 - bank_num_width - addr_width - 2 ,0) === io.ar_addr_M(16 , (bank_num_width  + addr_width + 2)) && tag_banks(ar_addr_reg(bank_num_width + 1 ,2)).rdata(16 - bank_num_width - addr_width - 2 + 1)
    io.hit_R := tag_banks(ar_addr_reg(bank_num_width + 1,2)).rdata(16 - bank_num_width - addr_width - 2 ,0) === io.ar_addr_R(16 , (bank_num_width  + addr_width + 2)) && tag_banks(ar_addr_reg(bank_num_width + 1 ,2)).rdata(16 - bank_num_width - addr_width - 2 + 1)
}
