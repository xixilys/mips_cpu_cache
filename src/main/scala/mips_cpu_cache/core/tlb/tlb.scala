package examples

import chisel3._
import chisel3.stage._
import chisel3.util._
import firrtl.PrimOps
import scala.math._
import scala.reflect.runtime.Macros
import scala.collection.mutable.ListBuffer

class tlb_write_or_read_port extends Bundle {

    def get_cdv1(value : UInt) : UInt = {value(4,0) }
    def get_pfn1(value : UInt) : UInt = {value(24,5) }
    def get_cdv0(value : UInt) : UInt = {value(29,25) }
    def get_pfn0(value : UInt) : UInt = {value(49,30) }
    def get_g(value:UInt):Bool = { value(50) }
    def get_asid(value:UInt):UInt = {value(58,51) }
    def get_vpn2(value:UInt):UInt = {value(77,59) }

    val vaddr = Input(UInt(19.W))
    val asid  = Input(UInt(8.W))
    val g     = Input(Bool())//代表是不是全局有效的tlb项
    val paddr = Vec(2,Input(UInt(20.W)))
    val c     = Vec(2,Input(UInt(3.W)))
    val d     = Vec(2,Input(Bool()))
    val v     = Vec(2,Input(Bool()))
    def tlb_port_to_UInt :UInt ={
        Cat(vaddr,asid,g,paddr(0),c(0),d(0),v(0),paddr(1),c(1),d(1),v(1))
    }
   
}


class tlb(length:Int) extends Module {

    val index_width = (log10(length)/log10(2)).toInt
    val io = IO(new Bundle{
        val   vaddr = Input(UInt(32.W))
        val   paddr = Output(UInt(32.W))
        val   cp0_asid  = Input(UInt(8.W))
        // val   vaddr_write = Input(UInt(20.W))
        // val   paddr_write = Vec(2,Input(UInt(20.W)))
        val   tlb_write_index = Input(UInt(index_width.W))
        val   tlb_read_index  = Input(UInt(index_width.W))
        
        val   tlb_write_en    = Input(Bool() )
        
        val   tlb_search_index = Output(UInt(index_width.W))
        //val   search_answer = Output(Bool())//代表有没有查到数据
        val   tlb_search_hit = Output(Bool())
        val   tlb_search_ineffective = Output(Bool())//1表示虽然命中但是无效
        val   tlb_search_has_changed = Output(Bool())

        val   tlb_write_port = (new tlb_write_or_read_port)
        val   tlb_read_port  = (Flipped(new tlb_write_or_read_port))

       
    })   

    def get_cdv1(value : UInt) : UInt = {value(4,0) }
    def get_pfn1(value : UInt) : UInt = {value(24,5) }
    def get_cdv0(value : UInt) : UInt = {value(29,25) }
    def get_pfn0(value : UInt) : UInt = {value(49,30) }
    def get_g(value:UInt):Bool = { value(50) }
    def get_asid(value:UInt):UInt = {value(58,51) }
    def get_vpn2(value:UInt):UInt = {value(77,59) }

    def UInt_to_tlb_port(value :UInt):Bundle = {
        val tlb_port = IO(Flipped(new tlb_write_or_read_port))
        tlb_port.v(1) := get_cdv1(value)(0)
        tlb_port.d(1) := get_cdv1(value)(1)
        tlb_port.c(1) := get_cdv1(value)(4,2)
        tlb_port.v(0) := get_cdv0(value)(0)
        tlb_port.v(0) := get_cdv0(value)(1)
        tlb_port.v(0) := get_cdv0(value)(4,2)
        tlb_port.paddr(0) := get_pfn0(value)
        tlb_port.paddr(1) := get_pfn1(value)
        tlb_port.vaddr    := get_vpn2(value)
        tlb_port.g        := get_g(value)
        tlb_port.asid     := get_asid(value)
        tlb_port
    }

    val tlb_reg = RegInit(VecInit(Seq.fill(length)(0.U(78.W))))
    val tlb_search_answer = Wire(Vec(length,Bool()))
    val tlb_search_phy = Wire(UInt(20.W))
    val tlb_search_hit = Wire(Bool())
    for( i <- 0 until length) {
        tlb_search_answer(i) := get_vpn2(tlb_reg(i)) === io.vaddr(31,13)
    }
    val tlb_search_value = Mux1H(tlb_search_answer,tlb_reg)
    // io.search_answer := tlb_search_answer.asUInt === 0.U 
//g位代表该表项为全局的tlb  

    var simuque: List[UInt] = List()

    for (i <- 0 until length) {
        simuque = i.asUInt :: simuque
    }
    val adding_list = simuque.reverse .toSeq
    tlb_search_hit := tlb_search_answer.asUInt =/= 0.U && (get_asid(tlb_search_value) === io.cp0_asid || get_g(tlb_search_value)) 
    

    tlb_search_phy := Mux(io.vaddr(12),get_pfn1(tlb_search_value),get_pfn0(tlb_search_value))

    tlb_reg(io.tlb_write_index) := Mux(io.tlb_write_en,io.tlb_write_port.tlb_port_to_UInt,tlb_reg(io.tlb_write_index))
    
    //val tlb_table = Module(new Look_up_table_read_first_(16,78)).io


    io.paddr := Cat(tlb_search_phy,io.vaddr(11,0))

    io.tlb_search_hit := tlb_search_hit
    io.tlb_search_ineffective := tlb_search_hit  &&  !Mux(io.vaddr(12),get_cdv1(tlb_search_value)(0),get_cdv0(tlb_search_value)(0))
    io.tlb_search_has_changed := tlb_search_hit  &&  Mux(io.vaddr(12),get_cdv1(tlb_search_value)(0),get_cdv0(tlb_search_value)(0)) &&
        !Mux(io.vaddr(12),get_cdv1(tlb_search_value)(1),get_cdv0(tlb_search_value)(1))

    val tlb_read_data = tlb_reg(io.tlb_read_index)
    io.tlb_read_port.v(1)     := get_cdv1(tlb_read_data)(0)
    io.tlb_read_port.d(1)     := get_cdv1(tlb_read_data)(1)
    io.tlb_read_port.c(1)     := get_cdv1(tlb_read_data)(4,2)
    io.tlb_read_port.v(0)     := get_cdv0(tlb_read_data)(0)
    io.tlb_read_port.d(0)     := get_cdv0(tlb_read_data)(1)
    io.tlb_read_port.c(0)     := get_cdv0(tlb_read_data)(4,2)
    io.tlb_read_port.paddr(0) := get_pfn0(tlb_read_data)
    io.tlb_read_port.paddr(1) := get_pfn1(tlb_read_data)
    io.tlb_read_port.vaddr    := get_vpn2(tlb_read_data)
    io.tlb_read_port.g        := get_g(tlb_read_data)
    io.tlb_read_port.asid     := get_asid(tlb_read_data)
    // io.tlb_read_port
    io.tlb_search_index := Mux1H(tlb_search_answer,adding_list)

}

// object tlb_test extends App{
//     // val some = new tlb(16)
//     // println(some.adding_list(0))
//     (new ChiselStage).emitVerilog(new tlb(16))
// }


