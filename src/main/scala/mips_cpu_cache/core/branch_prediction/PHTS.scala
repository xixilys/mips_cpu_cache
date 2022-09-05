package examples

import chisel3._
import chisel3.stage._
import chisel3.util._
import scala.math._


class  PHT(length : Int,width : Int) extends Module {
    val addr_width = (log10(length)/log10(2)).toInt
    val io = IO(new Bundle {
        val ar_addr  = Input(UInt(addr_width.W))
        val aw_addr  = Input(UInt(addr_width.W))
        val write = Input(Bool()) // 0 => 不写入 01 => 部分写入 10 => 全写入
        val in = Input(UInt(width.W))
        val out = Output(UInt(width.W))
    })  
    val put = RegInit(VecInit(Seq.fill(length)(0.U((width).W))))
    io.out := put(io.ar_addr)
    //io.out := Mux(io.write && io.aw_addr === io.ar_addr,io.in,put(io.aw_addr))
    for(  i <- 0 to length - 1) {
        put(i) := Mux(io.write && i.asUInt === io.aw_addr,io.in,put(i))
    }
}

class  PHTS (length : Int,width : Int ,ways: Int)  extends Module {
    val addr_width = (log10(length)/log10(2)).toInt
    val ways_width = (log10(ways)/log10(2)).toInt
    val io = IO(new Bundle {
        val ar_addr  = Input(UInt(addr_width.W))
        val ar_pht_addr = Input(UInt(ways_width.W))
        val aw_addr  = Input(UInt(addr_width.W))
        val aw_pht_addr = Input(UInt(ways_width.W))
        val write = Input(Bool()) // 0 => 不写入 01 => 部分写入 10 => 全写入
        val in = Input(UInt(width.W))
        val out = Output(UInt(width.W))
    })  

    //选择一直都是存width宽度的数据，尽量降低PHTS的复杂度
    //还没确定后面分支恢复的时候的时序，先按照不停流水线来做吧

    val phts = VecInit(Seq.fill(ways)(Module(new PHT(length,width)).io))
    for(i <- 0 until ways ) {
        phts(i).write := io.aw_pht_addr === i.asUInt &&  io.write
        phts(i).in := io.in
        phts(i).ar_addr := io.ar_addr
        phts(i).aw_addr := io.aw_addr
    }
    io.out := phts(io.ar_pht_addr).out
}


// 一组phts包含多少put，利用has算法来得出pht中的phts
// 四路组相连PHTS，根据输入pc的两个

class  PHTS_banks (length : Int,width : Int ,ways: Int,bank_num: Int)  extends Module {
    val addr_width = (log10(length)/log10(2)).toInt
    val ways_width = (log10(ways)/log10(2)).toInt
    val bank_num_width = (log10(bank_num)/log10(2)).toInt
    val io = IO(new Bundle { //分支指令不支持同时写
        val ar_bank_sel = Input(UInt(bank_num_width.W))
        val ar_addr_L  = Input(UInt(addr_width.W))
        val ar_addr_M  = Input(UInt(addr_width.W))
        val ar_addr_R  = Input(UInt(addr_width.W))
        val ar_pht_addr = Input(UInt(ways_width.W))
        val aw_addr  = Input(UInt(addr_width.W))
        val aw_pht_addr = Input(UInt(ways_width.W))
        val aw_bank_sel = Input(UInt(bank_num_width.W))
        val write = Input(Bool()) // 0 => 不写入 01 => 部分写入 10 => 全写入
        val in = Input(UInt(width.W))
        val out_L = Output(UInt(width.W))
        val out_M = Output(UInt(width.W))
        val out_R = Output(UInt(width.W))
    })


    //选择一直都是存width宽度的数据，尽量降低PHTS的复杂度
    //还没确定后面分支恢复的时候的时序，先按照不停流水线来做吧

    val phts_banks = VecInit(Seq.fill(bank_num)(Module(new PHTS(length,width,ways)).io))
    for(i <- 0 until bank_num ) {
        phts_banks(i).write := io.aw_bank_sel === i.asUInt &&  io.write
        phts_banks(i).in := io.in
        phts_banks(i).ar_addr := io.ar_addr_L//MuxLookup(i.asUInt,0.U,Seq(
        //     io.ar_bank_sel -> io.ar_addr_L,
        //     (io.ar_bank_sel + 1.U)(bank_num_width - 1,0) -> io.ar_addr_M,
        //     (io.ar_bank_sel + 2.U)(bank_num_width - 1,0) -> io.ar_addr_R
        // ))
        phts_banks(i).ar_pht_addr := io.ar_pht_addr//这个应该就是直接用pc中的中间20位生成的一个hascode来进行寻址
        phts_banks(i).aw_addr := io.aw_addr
        phts_banks(i).aw_pht_addr := io.aw_pht_addr
    }
    io.out_L := phts_banks(io.ar_bank_sel).out
    io.out_M := phts_banks(io.ar_bank_sel).out//phts_banks((io.ar_bank_sel + 1.U)((bank_num_width - 1),0)).out
    io.out_R := phts_banks(io.ar_bank_sel).out//phts_banks((io.ar_bank_sel + 2.U)((bank_num_width - 1),0)).out
   // io.out := phts(io.ar_pht_addr).out
}

// object PHTS_banks_test extends App{
//     (new ChiselStage).emitVerilog(new PHTS_banks(128,2,128,4))
// }
class  PHTS_banks_oneissue (length : Int,width : Int ,ways: Int,bank_num: Int)  extends Module {
    val addr_width = (log10(length)/log10(2)).toInt
    val ways_width = (log10(ways)/log10(2)).toInt
    val bank_num_width = (log10(bank_num)/log10(2)).toInt
    val io = IO(new Bundle { //分支指令不支持同时写
        val ar_bank_sel = Input(UInt(bank_num_width.W))
        val ar_addr_L  = Input(UInt(addr_width.W))
        val ar_addr_M  = Input(UInt(addr_width.W))
        val ar_addr_R  = Input(UInt(addr_width.W))
        val ar_pht_addr = Input(UInt(ways_width.W))
        val aw_addr  = Input(UInt(addr_width.W))
        val aw_pht_addr = Input(UInt(ways_width.W))
        val aw_bank_sel = Input(UInt(bank_num_width.W))
        val write = Input(Bool()) // 0 => 不写入 01 => 部分写入 10 => 全写入
        val in = Input(UInt(width.W))
        val out_L = Output(UInt(width.W))
        val out_M = Output(UInt(width.W))
        val out_R = Output(UInt(width.W))
    })


    //选择一直都是存width宽度的数据，尽量降低PHTS的复杂度
    //还没确定后面分支恢复的时候的时序，先按照不停流水线来做吧

    val phts_banks = VecInit(Seq.fill(bank_num)(Module(new PHTS(length,width,ways)).io))
    for(i <- 0 until bank_num ) {
        phts_banks(i).write := io.aw_bank_sel === i.asUInt &&  io.write
        phts_banks(i).in := io.in
        phts_banks(i).ar_addr := io.ar_addr_L//MuxLookup(i.asUInt,0.U,Seq(
           // io.ar_bank_sel -> ,
     
        phts_banks(i).ar_pht_addr := io.ar_pht_addr//这个应该就是直接用pc中的中间20位生成的一个hascode来进行寻址
        phts_banks(i).aw_addr := io.aw_addr
        phts_banks(i).aw_pht_addr := io.aw_pht_addr
    }
    io.out_L := phts_banks(io.ar_bank_sel).out
    io.out_M := phts_banks(io.ar_bank_sel).out//phts_banks((io.ar_bank_sel + 1.U)((bank_num_width - 1),0)).out
    io.out_R := phts_banks(io.ar_bank_sel).out//phts_banks((io.ar_bank_sel + 2.U)((bank_num_width - 1),0)).out
   // io.out := phts(io.ar_pht_addr).out
}


class pht_data_ram(length : Int) extends BlackBox {
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

class pht_data_with_block_ram(length : Int)  extends Module with mips_macros {
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
    val btb_data_ram_0 = Module(new pht_data_ram(length))
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

class  PHTS_with_block_ram (length : Int,ways: Int)  extends Module {
    val addr_width = (log10(length)/log10(2)).toInt
    val ways_width = (log10(ways)/log10(2)).toInt
    val io = IO(new Bundle {
        val ar_addr  = Input(UInt(addr_width.W))
        val ar_pht_addr = Input(UInt(ways_width.W))
        val aw_addr  = Input(UInt(addr_width.W))
        val aw_pht_addr = Input(UInt(ways_width.W))
        val write = Input(Bool()) // 0 => 不写入 01 => 部分写入 10 => 全写入
        val in = Input(UInt(8.W))
        val pht_out = Output(UInt(8.W))
        val out = Output(UInt(2.W))
    })  

    //选择一直都是存width宽度的数据，尽量降低PHTS的复杂度
    //还没确定后面分支恢复的时候的时序，先按照不停流水线来做吧

    val phts = VecInit(Seq.fill(ways)(Module(new pht_data_with_block_ram(length/4)).io))
    for(i <- 0 until ways ) {
        phts(i).wen := io.aw_pht_addr === i.asUInt &&  io.write
        phts(i).en  := 1.U
        phts(i).wdata := io.in
        phts(i).raddr := io.ar_addr(addr_width - 1,2)
        phts(i).waddr := io.aw_addr(addr_width - 1,2)
    }
    val raddr_reg = RegInit(0.U(addr_width.W))
    val ways_araddr_reg = RegInit(0.U(ways.W))
    raddr_reg := io.ar_addr
    ways_araddr_reg := io.ar_pht_addr
    io.out := MuxLookup(raddr_reg(1,0),phts(ways_araddr_reg).rdata(7,6),Seq(
        0.U ->  phts(ways_araddr_reg).rdata(1,0),
        1.U ->  phts(ways_araddr_reg).rdata(3,2),
        2.U ->  phts(ways_araddr_reg).rdata(5,4)
    ))    
    io.pht_out := phts(ways_araddr_reg).rdata
    // phts(io.ar_pht_addr).rdata
}
class  PHTS_banks_oneissue_block_ram (length : Int,width : Int ,ways: Int,bank_num: Int)  extends Module {
    val addr_width = (log10(length)/log10(2)).toInt
    val ways_width = (log10(ways)/log10(2)).toInt
    val bank_num_width = (log10(bank_num)/log10(2)).toInt
    val io = IO(new Bundle { //分支指令不支持同时写
        val ar_bank_sel = Input(UInt(bank_num_width.W))
        val ar_addr_L  = Input(UInt(addr_width.W))
        val ar_addr_M  = Input(UInt(addr_width.W))
        val ar_addr_R  = Input(UInt(addr_width.W))
        val ar_pht_addr = Input(UInt(ways_width.W))
        val aw_addr  = Input(UInt(addr_width.W))
        val aw_pht_addr = Input(UInt(ways_width.W))
        val aw_bank_sel = Input(UInt(bank_num_width.W))
        val write = Input(Bool()) // 0 => 不写入 01 => 部分写入 10 => 全写入
        val in = Input(UInt(8.W))
        val out_L = Output(UInt(width.W))
        val out_M = Output(UInt(width.W))
        val out_R = Output(UInt(width.W))
        val pht_out = Output(UInt(8.W))
    })


    //选择一直都是存width宽度的数据，尽量降低PHTS的复杂度
    //还没确定后面分支恢复的时候的时序，先按照不停流水线来做吧

    val phts_banks = VecInit(Seq.fill(bank_num)(Module(new PHTS_with_block_ram(length,ways)).io))
    for(i <- 0 until bank_num ) {
        phts_banks(i).write := io.aw_bank_sel === i.asUInt &&  io.write
        phts_banks(i).in := io.in
        phts_banks(i).ar_addr := io.ar_addr_L//MuxLookup(i.asUInt,0.U,Seq(
           // io.ar_bank_sel -> ,
     
        phts_banks(i).ar_pht_addr := io.ar_pht_addr//这个应该就是直接用pc中的中间20位生成的一个hascode来进行寻址
        phts_banks(i).aw_addr := io.aw_addr
        phts_banks(i).aw_pht_addr := io.aw_pht_addr
    }
    val ar_bank_sel_reg = RegInit(0.U(bank_num_width.W))
    ar_bank_sel_reg := io.ar_bank_sel
    io.out_L := phts_banks(ar_bank_sel_reg).out
    io.out_M := phts_banks(ar_bank_sel_reg).out//phts_banks((io.ar_bank_sel + 1.U)((bank_num_width - 1),0)).out
    io.out_R := phts_banks(ar_bank_sel_reg).out//phts_banks((io.ar_bank_sel + 2.U)((bank_num_width - 1),0)).out
    io.pht_out := phts_banks(ar_bank_sel_reg).pht_out

   // io.out := phts(io.ar_pht_addr).out
}