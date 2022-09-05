package examples

import chisel3._
import chisel3.stage._
import chisel3.util._

//改成了虚拟地址cache，那么mmu的地址转换功能没用了
//
class addr_cal extends Module with mips_macros {//
        //完全没用到chisel真正好的地方，我是废物呜呜呜呜
    val io = IO(new Bundle {
       
        val     d_vaddr = Input(UInt(32.W))//虚地址
        val     d_width = Input(UInt(2.W))
        val     d_en = Input(UInt(1.W))
        val     d_clr = Input(UInt(1.W))
        val     d_memrl = Input(UInt(2.W))
       
        val     d_paddr = Output((UInt(32.W)))
        val     d_cached = Output(UInt(1.W))
        val     d_unaligned = Output(UInt(1.W))
    })

    io.d_paddr := io.d_vaddr
    io.d_cached := Mux(io.d_clr.asBool,0.U,Mux(io.d_en.asBool,check_cached(io.d_vaddr),0.U))//d_cached_Reg
    io.d_unaligned := check_unaligned(io.d_width,io.d_vaddr(1,0),io.d_memrl)//d_unaligned_Reg
   
} 
// object mmu_test extends App{
//     (new ChiselStage).emitVerilog(new mmu)
// }


