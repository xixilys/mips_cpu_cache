package test 
import chisel3._

class REG extends Module {

    val io = IO(new Bundle{
        val a  = Input(UInt(8.W))
        val en = Input(Bool())
        val c  = Output(UInt(1.W))
    })
    val reg0 = RegNext(io.a)
    
}