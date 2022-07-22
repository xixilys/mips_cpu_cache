package examples

import chisel3._
import chisel3.stage._
import chisel3.util._

class br extends Module with mips_macros { //应该就是在id时就开始准备清空流水线或者跳转了
        
    val io = IO(new Bundle { 
        val en  = Input(UInt(1.W))

        val rs  = Input(UInt(32.W))
        val rt  = Input(UInt(32.W))
        val branch  = Input(UInt(6.W))

        val exe   = Output(UInt(1.W))
    })
    val rs_Wire = Wire(UInt(32.W))
    val rt_Wire = Wire(UInt(32.W))
    val branch_Wire = Wire(UInt(32.W))

    rs_Wire :=  Mux(io.en.asBool,io.rs,0.U)
    rt_Wire :=  Mux(io.en.asBool,io.rt,0.U)
    branch_Wire := Mux(io.en.asBool,io.branch,0.U)




    val result = Cat((rs_Wire.asSInt < 0.asSInt),
                    (rs_Wire.asSInt <= 0.asSInt),
                    (rs_Wire.asSInt > 0.asSInt),
                    (rs_Wire.asSInt >= 0.asSInt),
                    (rs_Wire =/= rt_Wire ),
                    (rs_Wire === rt_Wire))   //正好代表branch量中不同位代表不同意思 ,具体看macros
    io.exe := io.en.asBool && !reset.asBool && ((result & io.branch) =/= 0.U)
}

// object br_test extends App{
//     (new ChiselStage).emitVerilog(new br)
// }

