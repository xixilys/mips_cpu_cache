package examples

import chisel3._
import chisel3.stage._
import chisel3.util._

class dcache_data_ram extends BlackBox {
    val io = IO(new Bundle {
     
    val        clka = Input(UInt(1.W))
    val        ena  = Input(UInt(1.W))
    val        wea   = Input(UInt(4.W))
    val        addra   = Input(UInt(7.W))
    val        dina  = Input(UInt(32.W))
    val        douta  = Output(UInt(32.W))
  
    })
}


class dcache_data  extends Module with mips_macros {
        //完全没用到chisel真正好的地方，我是废物呜呜呜呜
    val io = IO(new Bundle {
     
    val        en   = Input(UInt(1.W))
    val        wen   = Input(UInt(4.W))
    val        addr   = Input(UInt(32.W))
    val        wdata   = Input(UInt(32.W))
    val        rdata  = Output(UInt(32.W))
  
    })
    val dcache_data_ram_0 = Module(new dcache_data_ram)
    dcache_data_ram_0.io.clka := clock.asUInt
    dcache_data_ram_0.io.ena   := io.en
    dcache_data_ram_0.io.wea  := io.wen
    dcache_data_ram_0.io.addra := io.addr(11,5)
    dcache_data_ram_0.io.dina := io.wdata
    io.rdata     := dcache_data_ram_0.io.douta 


}
// object dcache_data_test extends App{
//     (new ChiselStage).emitVerilog(new dcache_data)
// }


