package examples

import chisel3._
import chisel3.stage._
import chisel3.util._


class mmu extends Module with mips_macros {//
        //完全没用到chisel真正好的地方，我是废物呜呜呜呜
    val io = IO(new Bundle {
        val     i_vaddr =Input(UInt(32.W))
        val     i_en = Input(UInt(1.W))
    //    val    i_clr = (UInt(32.W))
        val     d_vaddr = Input(UInt(32.W))//虚地址
        val     d_width = Input(UInt(2.W))
        val     d_en = Input(UInt(1.W))
        val     d_clr = Input(UInt(1.W))
        val     d_memrl = Input(UInt(2.W))

        val     i_paddr = Output(UInt(32.W))
        val     i_cached = Output(UInt(1.W))
        val     i_unaligned = Output(UInt(1.W))
        val     d_paddr = Output((UInt(32.W)))
        val     d_cached = Output(UInt(1.W))
        val     d_unaligned = Output(UInt(1.W))
    })
    def check_unaligned(width:UInt,rd:UInt,memrl:UInt):UInt = MuxCase(1.U,Seq(
        (Cat(width,rd(0)) === "b100".U) -> 0.U,
        (Cat(width,rd) === "b1100".U) -> 0.U,
        (width ===       1.U )->0.U,
        (memrl =/= 0.U) -> 0.U))
    def check_cached(address:UInt) :UInt = Mux(address(31,29) === "b100".U  ,1.U,0.U )
    def memory_mapping(address:UInt): UInt = Mux((address(31,29) === "b100".U||
        address(31,29) === "b101".U),Cat(0.U(3.W),address(28,0)),address)//只有0x8-0x9 and 0xa-0xb为unmapped，可以直接线性映射
    // val   i_paddr_Reg = RegInit(Cat(0xbfc0.U(16.W),0x0000.U(16.W)))
    // val   i_cached_Reg = RegInit(0.U(1.W))
    // val   i_unaligned_Reg = RegInit(0.U(1.W))
    // val   d_paddr_Reg = RegInit(0.U(32.W))
    // val   d_cached_Reg = RegInit(0.U(1.W))
    // val   d_unaligned_Reg = RegInit(0.U(1.W)) 
    io.i_paddr := Mux(io.i_en.asBool,memory_mapping(io.i_vaddr),Cat(0x1fc0.U(16.W),0x0000.U(16.W)))//i_paddr_Reg
    io.d_paddr := Mux(io.d_clr.asBool,0.U,Mux(io.d_en.asBool,memory_mapping(io.d_vaddr),0.U))
    io.i_cached := Mux(io.i_en.asBool,check_cached(io.i_vaddr),0.U)//i_cached_Reg
    io.d_cached := Mux(io.d_clr.asBool,0.U,Mux(io.d_en.asBool,check_cached(io.d_vaddr),0.U))//d_cached_Reg
    io.d_unaligned := Mux(io.d_clr.asBool,0.U,Mux(io.d_en.asBool,check_unaligned(io.d_width,io.d_vaddr(1,0),io.d_memrl),0.U))//d_unaligned_Reg
    io.i_unaligned := Mux(io.i_en.asBool,Mux(io.i_vaddr(1,0)===0.U,0.U,1.U),0.U)//i_unaligned_Reg
    // i_paddr_Reg := Mux(io.i_en.asBool,memory_mapping(io.i_vaddr),i_paddr_Reg)
    // i_cached_Reg :=  Mux(io.i_en.asBool,check_cached(io.i_vaddr),i_paddr_Reg)
    // i_unaligned_Reg := Mux(io.i_en.asBool,Mux(io.i_vaddr(1,0)===0.U,0.U,1.U),i_unaligned_Reg)//指令恒为4字节

    // d_paddr_Reg := Mux(io.d_clr.asBool,0.U,Mux(io.d_en.asBool,memory_mapping(io.d_vaddr),d_paddr_Reg))
    // d_cached_Reg :=  Mux(io.d_clr.asBool,0.U,Mux(io.d_en.asBool,check_cached(io.d_vaddr),d_paddr_Reg))
    // d_unaligned_Reg := Mux(io.d_clr.asBool,0.U,Mux(io.d_en.asBool,Mux(io.d_vaddr(1,0)===0.U,0.U,1.U),d_unaligned_Reg))//指令恒为4字节
} 
// object mmu_test extends App{
//     (new ChiselStage).emitVerilog(new mmu)
// }


