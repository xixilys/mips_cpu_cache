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

