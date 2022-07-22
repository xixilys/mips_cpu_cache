package examples

import chisel3._
import chisel3.stage._
import chisel3.util._
import firrtl.PrimOps
import scala.math._

//整体分支预测采用竞争型的分支预测算法，包括基于局部历史的分支预测和基于全局历史的分支预测
class BHT(length : Int,width : Int) extends Module{
    val addr_width = (log10(length)/log10(2)).toInt
    val io = IO(new Bundle {
        val ar_addr  = Input(UInt(addr_width.W))
        val aw_addr  = Input(UInt(addr_width.W))
        val write = Input(Bool()) // 0 => 不写入 01 => 部分写入 10 => 全写入
        val in = Input(UInt(width.W))
        val out = Output(UInt(width.W))
    })  
    val bht = RegInit(VecInit(Seq.fill(length)(0.U((width).W))))
    io.out := Mux(io.write && io.aw_addr === io.ar_addr,io.in,bht(io.aw_addr))
    for(  i <- 0 to length - 1) {
        bht(i) := Mux(io.write && i.asUInt === io.aw_addr,io.in,bht(i))
    }
}

class  BHT_banks (length : Int,width : Int ,bank_num: Int)  extends Module {
    val addr_width = (log10(length)/log10(2)).toInt
    val bank_num_width = (log10(bank_num)/log10(2)).toInt
    val io = IO(new Bundle { //分支指令不支持同时写
        val ar_bank_sel = Input(UInt(bank_num_width.W))
        val ar_addr_L  = Input(UInt(addr_width.W))
        val ar_addr_M = Input(UInt(addr_width.W))
        val ar_addr_R = Input(UInt(addr_width.W))
        val aw_addr  = Input(UInt(addr_width.W))
        val write = Input(Bool()) 
        val in = Input(UInt(width.W))
        val out_L = Output(UInt(width.W))
        val out_M = Output(UInt(width.W))
        val out_R = Output(UInt(width.W))
    })
    //选择一直都是存width宽度的数据，尽量降低PHTS的复杂度ar_pht_addr
    //还没确定后面分支恢复的时候的时序，先按照不停流水线来做吧

    val bht_banks = VecInit(Seq.fill(bank_num)(Module(new BHT(length,width)).io))
    for(i <- 0 until bank_num ) {
        bht_banks(i).write := io.ar_bank_sel === i.asUInt &&  io.write
        bht_banks(i).in := io.in
        bht_banks(i).ar_addr := MuxLookup(i.asUInt,0.U,Seq(
            io.ar_bank_sel -> io.ar_addr_L,
            (io.ar_bank_sel + 1.U)(bank_num_width - 1,0) -> io.ar_addr_M,
            (io.ar_bank_sel + 2.U)(bank_num_width - 1,0) -> io.ar_addr_R
        ))
        bht_banks(i).aw_addr := io.aw_addr
    }
    io.out_L := bht_banks(io.ar_bank_sel).out
    io.out_M := bht_banks((io.ar_bank_sel + 1.U)((bank_num_width - 1),0)).out
    io.out_R := bht_banks((io.ar_bank_sel + 2.U)((bank_num_width - 1),0)).out
    
   // io.out := phts(io.ar_pht_addr).out
}
// object BHT_banks_test extends App{
//     (new ChiselStage).emitVerilog(new BHT_banks(128,7,4))
// }


