package examples

import chisel3._
import chisel3.stage._
import chisel3.util._


class hilo extends Module with mips_macros {//hi,lo寄存器
        
    val io = IO(new Bundle { 

        val  we = Input(UInt(2.W))
        val  hi_i = Input(UInt(32.W))
        val  lo_i = Input(UInt(32.W))
    
        val  hi_o = Output(UInt(32.W))
        val  lo_o= Output(UInt(32.W))

    })
    val hi_o_Reg   =  RegInit(0.U(32.W))
    val lo_o_Reg   =  RegInit(0.U(32.W))
    io.hi_o     := hi_o_Reg
    io.lo_o     := lo_o_Reg

    lo_o_Reg := Mux(io.we(0),io.lo_i,lo_o_Reg)
    hi_o_Reg := Mux(io.we(1),io.hi_i,hi_o_Reg)

}


// object hilo_test extends App{
//     (new ChiselStage).emitVerilog(new hilo)
// }


