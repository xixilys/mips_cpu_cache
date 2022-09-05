package examples

import chisel3._
import chisel3.stage._
import chisel3.util._

class alu extends Module with mips_macros {
        
    val io = IO(new Bundle { 

        val ctrl  = Input(UInt(24.W)) // 使用独热码来进行控制

        val in1   = Input(UInt(32.W))
        val in2   = Input(UInt(32.W))

        val result = Output(UInt(32.W))
        val overflow = Output(UInt(1.W))

    })
   // val result_Reg = RegInit(0.U(32.W))
    // io.result     :=  result_Reg//给输出加上寄存器
    val sa         = io.in1(4,0) 

    val answer_and   =  io.in1 & io.in2
    val answer_or    =  io.in1 | io.in2
    val answer_xor   =  io.in1 ^ io.in2
    val answer_nor   =  ~answer_or  //或非

    val answer_slt   = Mux((io.in1.asSInt <  io.in2.asSInt),1.U,0.U)//有符号小于比较
    val answer_sltu  = Mux((io.in1 <  io.in2),1.U,0.U)//无符号小于比较
    val answer_sll   = io.in2 << sa

    val answer_srl   = io.in2 >> sa
    val answer_sra   = (io.in2.asSInt >> sa).asUInt

    val answer_lui   = Cat(io.in2(15,0),0.U(16.W)) // 输入2

    val in1_extend   = Cat(io.in1(31),io.in1)
    val in2_extend   = Cat(io.in2(31),io.in2)

    val answer_add   = in1_extend + in2_extend  //使用变形补码法来判断溢出
    val answer_sub   = in1_extend - in2_extend 

    io.result := Mux1H(Seq(
        io.ctrl(ALU_ADD)    -> answer_add(31,0),
        io.ctrl(ALU_ADDE)   -> answer_add(31,0),
        io.ctrl(ALU_ADDU)   -> answer_add(31,0),
        io.ctrl(ALU_SUB)    -> answer_sub(31,0),
        io.ctrl(ALU_SUBE)   -> answer_sub(31,0),
        io.ctrl(ALU_SUBU)   -> answer_sub(31,0),
        io.ctrl(ALU_AND)    -> answer_and,
        io.ctrl(ALU_OR)     -> answer_or,
        io.ctrl(ALU_NOR)    -> answer_nor,
        io.ctrl(ALU_XOR)    -> answer_xor,
        io.ctrl(ALU_LUI)    -> answer_lui,
        io.ctrl(ALU_SLL)    -> answer_sll,
        io.ctrl(ALU_SLT)    -> answer_slt,
        io.ctrl(ALU_SRA)    -> answer_sra,
        io.ctrl(ALU_SRL)    -> answer_srl,
        io.ctrl(ALU_SLTU)   -> answer_sltu
    ))
    io.overflow := (io.ctrl(ALU_ADDE) && (answer_add(32) =/= answer_add(31))) || (io.ctrl(ALU_SUBE) && (answer_sub(32) =/= answer_sub(31)))
}
// object alu_test extends App{
//     (new ChiselStage).emitVerilog(new alu)
// }

