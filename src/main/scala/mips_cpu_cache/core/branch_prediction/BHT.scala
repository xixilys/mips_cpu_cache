package examples

import chisel3._
import chisel3.stage._
import chisel3.util._
import firrtl.PrimOps
import scala.math._

//整体分支预测采用竞争型的分支预测算法，包括基于局部历史的分支预测和基于全局历史的分支预测
//读优先
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
    io.out := bht(io.ar_addr)
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

class  BHT_banks_oneissue (length : Int,width : Int ,bank_num: Int)  extends Module {
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
        bht_banks(i).ar_addr := io.ar_addr_L
            //,//MuxLookup(i.asUInt,0.U,Seq(
            //io.ar_bank_sel -> 
        //     (io.ar_bank_sel + 1.U)(bank_num_width - 1,0) -> io.ar_addr_M,
        //     (io.ar_bank_sel + 2.U)(bank_num_width - 1,0) -> io.ar_addr_R
        // ))
        bht_banks(i).aw_addr := io.aw_addr
    }
    io.out_L := bht_banks(io.ar_bank_sel).out
    io.out_M := bht_banks(io.ar_bank_sel).out//bht_banks((io.ar_bank_sel + 1.U)((bank_num_width - 1),0)).out
    io.out_R := bht_banks(io.ar_bank_sel).out//bht_banks((io.ar_bank_sel + 2.U)((bank_num_width - 1),0)).out
    
   // io.out := phts(io.ar_pht_addr).out
}

class bht_data_ram(length : Int) extends BlackBox {
     val addr_width = (log10(length)/log10(2)).toInt
    val io = IO(new Bundle {
     
    val        clka   = Input(UInt(1.W))
    val        clkb   = Input(UInt(1.W))
    val        ena    = Input(UInt(1.W))
    val        enb    = Input(UInt(1.W))
    val        wea    = Input(UInt(1.W)) //没有使能对于字的写
    //a端口为读 端口为写
    val        addra  = Input(UInt(addr_width.W))
    val        dina   = Input(UInt(8.W))
    val        addrb  = Input(UInt(addr_width.W))
    val        doutb  = Output(UInt(8.W))
  
    })
}

class bht_data_with_block_ram(length : Int)  extends Module with mips_macros {
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
    val btb_data_ram_0 = Module(new bht_data_ram(length))
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


class  BHT_banks_oneissue_block_ram (length : Int,width : Int ,bank_num: Int)  extends Module {
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

    val bht_banks = VecInit(Seq.fill(bank_num)(Module(new bht_data_with_block_ram(length)).io))
    for(i <- 0 until bank_num ) {
        bht_banks(i).wen := io.ar_bank_sel === i.asUInt &&  io.write
        bht_banks(i).en := 1.U
        bht_banks(i).wdata := io.in
        bht_banks(i).raddr := io.ar_addr_L
            //,//MuxLookup(i.asUInt,0.U,Seq(
            //io.ar_bank_sel -> 
        //     (io.ar_bank_sel + 1.U)(bank_num_width - 1,0) -> io.ar_addr_M,
        //     (io.ar_bank_sel + 2.U)(bank_num_width - 1,0) -> io.ar_addr_R
        // ))
        bht_banks(i).waddr := io.aw_addr
    }
    val bht_bank_sel_reg = RegInit(0.U(bank_num_width.W))
    bht_bank_sel_reg := io.ar_bank_sel
    io.out_L := bht_banks(io.ar_bank_sel).rdata
    io.out_M := bht_banks(io.ar_bank_sel).rdata//bht_banks((io.ar_bank_sel + 1.U)((bank_num_width - 1),0)).out
    io.out_R := bht_banks(io.ar_bank_sel).rdata//bht_banks((io.ar_bank_sel + 2.U)((bank_num_width - 1),0)).out
    
   // io.out := phts(io.ar_pht_addr).out
}
