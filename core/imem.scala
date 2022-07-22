package examples

import chisel3._
import chisel3.stage._
import chisel3.util._


class imem extends Module with mips_macros { //数据mem
        
    val io = IO(new Bundle { 
    // val               clk = Input(UInt(1.W))
    // val               rst = Input(UInt(1.W))
    
    val               inst_req = Input(UInt(1.W))
    val               inst_addr_ok = Input(UInt(1.W))
    val               inst_data_ok = Input(UInt(1.W))
    val               inst_hit = Input(UInt(1.W))
    val               inst_rdata = Input(UInt(32.W))
    val               inst_cache = Input(UInt(1.W))

    val               InstUnalignedF = Input(UInt(1.W))
    val               ReadDataF   = Output(UInt(32.W))
    })
    val RD_Reg = RegInit(0.U(32.W))
    val inst_cache_Reg = RegInit(0.U(1.W))
    inst_cache_Reg := io.inst_cache.asBool
    val hit = inst_cache_Reg.asBool && io.inst_hit.asBool 
    // val data_ok_reg = RegInit(0.U(1.W))
    // data_ok_reg := io.inst_data_ok
    io.ReadDataF   := Mux(io.InstUnalignedF.asBool,0.U,Mux(io.inst_data_ok.asBool ||hit ,io.inst_rdata,RD_Reg))//是想要数据一对就马上输入吗,需要商榷
    RD_Reg  := Mux(io.inst_data_ok.asBool ||hit,io.inst_rdata,RD_Reg)
    // io.RD   := RD_Reg
}


// object imem_test extends App{
//     (new ChiselStage).emitVerilog(new imem)
// }


