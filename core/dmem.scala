package examples

import chisel3._
import chisel3.stage._
import chisel3.util._


class dmem extends Module with mips_macros {//hi = Input(UInt(32.W))lo寄存器
        //完全没用到chisel真正好的地方，我是废物呜呜呜呜
    val io = IO(new Bundle {
    val    req     = Input(UInt(1.W))
    val    addr_ok = Input(UInt(1.W))
    val    data_ok = Input(UInt(1.W))
    val    rdata   = Input(UInt(32.W))

    val    ReadEn      = Input(UInt(1.W)) 
    val    Physisc_Address       = Input(UInt(32.W)) //p Physisc_Address
    val    WIDTH   = Input(UInt(2.W)) 
    val    SIGN    = Input(UInt(1.W))
    
    val    RD      = Output(UInt(32.W))
    val    data_pending = Output(UInt(1.W))
    })
    // val RD_Reg = RegInit(0.U(32.W))
    val data_pending_Reg = RegInit(0.U(1.W))
    val pending_reg = RegInit(0.U(1.W))
    pending_reg := data_pending_Reg
    io.data_pending := data_pending_Reg

    data_pending_Reg := Mux(((!io.data_ok.asBool) && io.req.asBool),1.U, //req应该仅仅保持一个周期
            Mux(io.data_ok.asBool,0.U,data_pending_Reg))
    val ra = io.Physisc_Address(1,0)
    val data_ok_reg = RegInit(0.U(1.W))
    data_ok_reg := io.data_ok
    val rdata_reg = RegInit(0.U(32.W))
    rdata_reg := io.rdata

    def get_byte(data:UInt,offset:UInt,sign:UInt):UInt = MuxLookup(Cat(offset,sign),0.U,Seq(
        "b00_1".U -> sign_extend(data(7,0),8),
        "b00_0".U -> unsign_extend(data(7,0),8),
        "b01_1".U -> sign_extend(data(15,8),8),
        "b01_0".U -> unsign_extend(data(15,8),8),
        "b10_1".U -> sign_extend(data(23,16),8),
        "b10_0".U -> unsign_extend(data(23,16),8),
        "b11_1".U -> sign_extend(data(31,24),8),
        "b11_0".U -> unsign_extend(data(31,24),8)))

    def get_halfword(data:UInt,offset:UInt,sign:UInt):UInt = MuxLookup(Cat(offset,sign),0.U,Seq(
        "b00_1".U -> sign_extend(data(15,0),16),
        "b00_0".U -> unsign_extend(data(15,0),16),
        "b10_1".U -> sign_extend(data(31,16),16),
        "b10_0".U -> unsign_extend(data(31,16),16)))

    val true_data = Mux(pending_reg.asBool,rdata_reg,io.rdata) //；为了满足cache命中，一周期读取完的要求，并且未命中可以接上cache的那边的时序

    val all_ok = (data_ok_reg.asBool && io.ReadEn.asBool)
    io.RD := Mux(all_ok,MuxLookup(io.WIDTH,0.U,Seq(
        1.U -> get_byte(true_data,ra,io.SIGN),
        2.U -> get_halfword(true_data,ra,io.SIGN),//有符号扩展或者无符号扩展
        3.U -> true_data )),0.U)
    
}
// object dmem_test extends App{
//     (new ChiselStage).emitVerilog(new dmem)
// }

