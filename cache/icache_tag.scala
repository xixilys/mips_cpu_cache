package examples

import chisel3._
import chisel3.stage._
import chisel3.util._


class icache_tag  extends Module with mips_macros {
        //完全没用到chisel真正好的地方，我是废物呜呜呜呜
    val io = IO(new Bundle {
     val        wen   = Input(UInt(1.W))//write en
     val        wdata   = Input(UInt(21.W))
     val        addr   = Input(UInt(32.W))
     val        hit   = Output(UInt(1.W))
     val        valid   = Output(UInt(1.W))
     val        op      = Input(UInt(1.W))
        
    })
    val tag_regs = RegInit(VecInit(Seq.fill(128)(0.U(21.W)))) //初始化寄存器
    val addr_reg = RegInit(0.U(32.W))
    addr_reg := io.addr
    tag_regs(io.addr(11,5)) := Mux(io.op.asBool||io.wen.asBool,io.wdata, tag_regs(io.addr(11,5)))
   // val tag_t = RegInit(0.U(32.W)) // 存疑
    val tag_t = tag_regs(io.addr(11,5)) 
    io.valid := tag_t(20) //tag_t(20)run
    io.hit := Mux(tag_t(19,0) === io.addr(31,12),1.U,0.U)//addr前20位全为tag
}
// object icache_tag_test extends App{
//     (new ChiselStage).emitVerilog(new icache_tag)
// }


