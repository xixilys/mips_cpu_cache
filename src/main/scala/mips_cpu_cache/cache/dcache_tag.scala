package examples

import chisel3._
import chisel3.stage._
import chisel3.util._


class dcache_tag  extends Module with mips_macros {
        //完全没用到chisel真正好的地方，我是废物呜呜呜呜
    val io = IO(new Bundle {
     val        wen   = Input(UInt(1.W))
     val        wdata   = Input(UInt(21.W))
     val        raddr   = Input(UInt(32.W))
     val        waddr   = Input(UInt(32.W))
     val        hit   = Output(UInt(1.W))
     val        valid   = Output(UInt(1.W))
     val        op      = Input(UInt(1.W))
     val        tag  =  Output(UInt(20.W))
        
    })
//     val tag_regs = RegInit(VecInit(Seq.fill(128)(0.U(21.W)))) //初始化寄存器
//     // val addr_reg = RegInit(0.U(32.W))
//     // addr_reg := io.addr
//     tag_regs(io.waddr(11,5)) := Mux(io.op.asBool||io.wen.asBool,io.wdata, tag_regs(io.waddr(11,5)))
//    // val tag_t = RegInit(0.U(32.W)) // 存疑
//     val tag_t = tag_regs(io.raddr(11,5)) 
//     io.tag := tag_t
//     io.valid := tag_t(20) //tag_t(20)run
//     io.hit := Mux(tag_t(19,0) === io.raddr(31,12),1.U,0.U)//addr前20位全为tag
    val tag_regs0 = RegInit(VecInit(Seq.fill(64)(0.U(21.W)))) //初始化寄存器
    val tag_regs1 = RegInit(VecInit(Seq.fill(64)(0.U(21.W)))) //初始化寄存器  
    // val addr_reg = RegInit(0.U(32.W))
    // addr_reg := io.addr
    tag_regs0(io.waddr(11,6)) := Mux((io.op.asBool||io.wen.asBool ) && !io.waddr(5),io.wdata, tag_regs0(io.waddr(11,6)))
    tag_regs1(io.waddr(11,6)) := Mux((io.op.asBool||io.wen.asBool ) && io.waddr(5),io.wdata, tag_regs1(io.waddr(11,6)))
   // val tag_t = RegInit(0.U(32.W)) // 存疑
    val tag_t0_write = tag_regs0(io.waddr(11,6)) 
    val tag_t1_write = tag_regs1(io.waddr(11,6))

    val tag_t0_read = tag_regs0(io.raddr(11,6)) 
    val tag_t1_read = tag_regs1(io.raddr(11,6))  
    io.tag := Mux(io.waddr(5),tag_t1_write(19,0),tag_t0_write(19,0))

    io.valid := Mux(io.raddr(5),tag_t1_read(20),tag_t0_read(20)) //tag_t(20)run
    io.hit := (io.raddr(5) && tag_t1_read(19,0) === io.raddr(31,12)) || (!io.raddr(5) && tag_t0_read(19,0) === io.raddr(31,12))
}

// object dcache_tag_test extends App{
//     (new ChiselStage).emitVerilog(new dcache_tag)
// }


// class dcache_  extends Module with mips_macros {
//         //完全没用到chisel真正好的地方，我是废物呜呜呜呜
//     val io = IO(new Bundle {
//      val        wen   = Input(UInt(1.W))
//      val        wdata   = Input(UInt(21.W))
//      val        raddr   = Input(UInt(32.W))
//      val        waddr   = Input(UInt(32.W))
//      val        hit   = Output(UInt(1.W))
//      val        valid   = Output(UInt(1.W))
//      val        op      = Input(UInt(1.W))
//      val        tag  =  Output(UInt(20.W))
        
//     })
// //     val tag_regs = RegInit(VecInit(Seq.fill(128)(0.U(21.W)))) //初始化寄存器
// //     // val addr_reg = RegInit(0.U(32.W))
// //     // addr_reg := io.addr
// //     tag_regs(io.waddr(11,5)) := Mux(io.op.asBool||io.wen.asBool,io.wdata, tag_regs(io.waddr(11,5)))
// //    // val tag_t = RegInit(0.U(32.W)) // 存疑
// //     val tag_t = tag_regs(io.raddr(11,5)) 
// //     io.tag := tag_t
// //     io.valid := tag_t(20) //tag_t(20)run
// //     io.hit := Mux(tag_t(19,0) === io.raddr(31,12),1.U,0.U)//addr前20位全为tag
//   val tag_regs0 = RegInit(VecInit(Seq.fill(64)(0.U(21.W)))) //初始化寄存器
//     val tag_regs1 = RegInit(VecInit(Seq.fill(64)(0.U(21.W)))) //初始化寄存器  
//     // val addr_reg = RegInit(0.U(32.W))
//     // addr_reg := io.addr
//     tag_regs0(io.waddr(11,6)) := Mux((io.op.asBool||io.wen.asBool ) && !io.waddr(5),io.wdata, tag_regs0(io.waddr(11,6)))
//     tag_regs1(io.waddr(11,6)) := Mux((io.op.asBool||io.wen.asBool ) && io.waddr(5),io.wdata, tag_regs1(io.waddr(11,6)))
//    // val tag_t = RegInit(0.U(32.W)) // 存疑
//     val tag_t0_write = tag_regs0(io.waddr(11,6)) 
//     val tag_t1_write = tag_regs1(io.waddr(11,6))

//     val tag_t0_read = tag_regs0(io.raddr(11,6)) 
//     val tag_t1_read = tag_regs1(io.raddr(11,6))  
//     io.tag := Mux(io.waddr(5),tag_t1_write(19,0),tag_t0_write(19,0))

//     io.valid := Mux(io.raddr(5),tag_t1_read(20),tag_t0_read(20)) //tag_t(20)run
//     io.hit := (io.raddr(5) && tag_t1_read(19,0) === io.raddr(31,12)) || (!io.raddr(5) && tag_t0_read(19,0) === io.raddr(31,12))
// }
// }
// object dcache_tag_test extends App{
//     (new ChiselStage).emitVerilog(new dcache_tag)
// }

// class dcache_tag  extends Module with mips_macros {
//         //完全没用到chisel真正好的地方，我是废物呜呜呜呜
//     val io = IO(new Bundle {
//      val        wen   = Input(UInt(1.W))//write en
//      val        wdata   = Input(UInt(21.W))
//      val        addr   = Input(UInt(32.W))
//      val        hit   = Output(UInt(1.W))
//      val        valid   = Output(UInt(1.W))
//      val        op      = Input(UInt(1.W))
//     //  val        asid = Input(UInt(8.W))
//     val        tag  =  Output(UInt(20.W))
        
//     })
//     val tag_regs = RegInit(VecInit(Seq.fill(128)(0.U(21.W)))) //初始化寄存器
//    // val tag_asid_regs = RegInit(VecInit(Seq.fill(128)(0.U(8.W))))
//     val addr_reg = RegInit(0.U(32.W))
//     addr_reg := io.addr
//     tag_regs(io.addr(11,5)) := Mux(io.op.asBool||io.wen.asBool,io.wdata, tag_regs(io.addr(11,5)))
//     //tag_asid_regs(io.addr(11,5)) := Mux(io.op.asBool||io.wen.asBool,io.asid,tag_asid_regs(io.addr(11,5)))
//    // val tag_t = RegInit(0.U(32.W)) // 存疑
//     val tag_t = tag_regs(io.addr(11,5)) 
//     io.tag := tag_t(19,0)
//     io.valid := tag_t(20) //tag_t(20)run
//     io.hit := tag_t(19,0) === io.addr(31,12) //&& tag_asid_regs(io.addr(11,5)) === io.asid,1.U,0.U)//addr前20位全为tag
// }
