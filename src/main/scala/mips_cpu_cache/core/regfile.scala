
package examples

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._

class regfile extends Module {
        
    val io = IO(new Bundle { //有隐式的时钟与复位，并且复位为高电平复位

        val A1   = Input(UInt(5.W))
        val A2   = Input(UInt(5.W))
        val WE3   = Input(UInt(1.W))//write enables 
        val A3   = Input(UInt(5.W))
        val WD3   = Input(UInt(32.W))

        val RD1    = Output(UInt(32.W))
        val RD2   = Output(UInt(32.W))
        
    })

    val regs = RegInit(VecInit(Seq.fill(32)(0.U(32.W)))) //初始化寄存器
    regs(0) := 0.U
    regs(io.A3) := Mux((io.WE3.asBool && io.A3 =/=0.U),io.WD3,regs(io.A3))
    val reg_search_answer_a1 = Wire(Vec(32,Bool()))
    val reg_search_answer_a2 = Wire(Vec(32,Bool()))
    for( i <- 0 until 32) {
        reg_search_answer_a1(i) := i.asUInt === io.A1
        reg_search_answer_a2(i) := i.asUInt === io.A2
    }
    //io.RD1 := Mux(io.WE3.asBool && io.A1 === io.A3,io.WD3,Mux1H(reg_search_answer_a1,regs))
    //io.RD2 := Mux(io.WE3.asBool && io.A2 === io.A3,io.WD3,Mux1H(reg_search_answer_a2,regs))
     io.RD1 := Mux(io.WE3.asBool && io.A1 === io.A3,io.WD3,regs(io.A1))
     io.RD2 := Mux(io.WE3.asBool && io.A2 === io.A3,io.WD3,regs(io.A2))
    //      MuxCase(regs(io.A2),Seq(
    //     (io.A2 === 0.U) -> 0.U,
    //     (io.WE3.asBool && io.A2 === io.A3) -> io.WD3
    // ))   
}

// object regfile_test extends App{
//     (new ChiselStage).emitVerilog(new regfile )
// }
