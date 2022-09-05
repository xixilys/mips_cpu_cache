package examples

import chisel3._
import chisel3.stage._
import chisel3.util._
import firrtl.PrimOps
import scala.math._
import scala.reflect.runtime.Macros
import scala.collection.mutable.ListBuffer

class cache_port extends Bundle {
    val vaddr = Input(UInt(32.W))        //val   vaddr = Input(UInt(32.W))
    val paddr = Output(UInt(32.W))
    // val size = Input(UInt(2.W))
    val req  = Input(Bool())
    // val cache = Input(Bool())
    val wr    = Input(Bool())
    val tlb_search_not_hit_exception = Output(Bool())
    val tlb_search_ineffective_exception = Output(Bool())//1表示虽然命中但是无效

}

class double_ports_tlb_for_inst_and_data(length:Int) extends Module {
 
    val index_width = (log10(length)/log10(2)).toInt
    val io = IO(new Bundle{

        val   cp0_asid  = Input(UInt(8.W))
     
        val   tlb_write_index  = Input(UInt(index_width.W))
        val   tlb_read_index   = Input(UInt(index_width.W))
        val   tlb_write_en     = Input(Bool() )
        val   tlb_search_index = Output(UInt(index_width.W))
        val   tlb_search_hit   = Output(Bool())
        

        //只有在store指令的时候才会触发
        //tlb修改例外
        val   tlb_dirty_exception = Output(Bool())

        val   tlb_write_port = (new tlb_write_or_read_port)
        val   tlb_read_port  = (Flipped(new tlb_write_or_read_port))

        val   icache_port = new cache_port
        val   dcache_port = new cache_port

    })   
    //两个tlb表格来模拟双端口
    val tlb_for_inst = Module(new tlb(length)).io
    val tlb_for_data = Module(new tlb(length)).io

    tlb_for_inst.vaddr := io.icache_port.vaddr
    tlb_for_data.vaddr := io.dcache_port.vaddr

    io.dcache_port.paddr := tlb_for_data.paddr
    io.icache_port.paddr := tlb_for_inst.paddr

    tlb_for_inst.cp0_asid := io.cp0_asid
    tlb_for_data.cp0_asid := io.cp0_asid
    
    tlb_for_inst.tlb_write_port  := io.tlb_write_port
    tlb_for_inst.tlb_write_en    := io.tlb_write_en
    tlb_for_inst.tlb_write_index := io.tlb_write_index
    tlb_for_inst.cp0_asid        := io.cp0_asid
    tlb_for_inst.tlb_read_index  := 0.U
    
    
    tlb_for_data.tlb_write_port  := io.tlb_write_port
    tlb_for_data.tlb_write_en    := io.tlb_write_en
    tlb_for_data.tlb_write_index := io.tlb_write_index
    tlb_for_data.cp0_asid        := io.cp0_asid
    tlb_for_data.tlb_read_index  := io.tlb_read_index
    io.tlb_read_port := tlb_for_data.tlb_read_port
    io.tlb_search_index := tlb_for_data.tlb_search_index
//req指发起tlb请求
//对于unmapped内存块当然不会发起请求

    io.icache_port.tlb_search_ineffective_exception := tlb_for_inst.tlb_search_ineffective && io.icache_port.req
    io.icache_port.tlb_search_not_hit_exception     := !tlb_for_inst.tlb_search_hit && io.icache_port.req //不命中

    io.dcache_port.tlb_search_ineffective_exception := tlb_for_data.tlb_search_ineffective && io.dcache_port.req
    io.dcache_port.tlb_search_not_hit_exception     := !tlb_for_data.tlb_search_hit && io.dcache_port.req

    io.tlb_dirty_exception  :=  io.dcache_port.wr && tlb_for_data.tlb_search_has_changed && io.dcache_port.req

    io.tlb_search_hit := tlb_for_data.tlb_search_hit

    
}
// object tlb_two_port_test extends App{
//     // val some = new tlb(16)
//     // println(some.adding_list(0))
//     (new ChiselStage).emitVerilog(new  double_ports_tlb_for_inst_and_data(16))
// }
