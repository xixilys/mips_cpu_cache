package examples

import chisel3._
import chisel3.stage._
import chisel3.util._


class dmemreq extends Module with mips_macros {//hi = Input(UInt(32.W))lo寄存器
        //完全没用到chisel真正好的地方，我是废物呜呜呜呜
    val io = IO(new Bundle { 
    val           en = Input(UInt(1.W))

    val     MemWriteE = Input(UInt(1.W))
    val     MemToRegE = Input(UInt(1.W))
    val     MemWidthE = Input(UInt(2.W))
    val     PhyAddrE = Input(UInt(32.W))
    val     WriteDataE = Input(UInt(32.W))
    val     addr_ok = Input(UInt(1.W))
    // val     MemRLE   = Input(UInt(2.W))

    val     req = Output(UInt(1.W))
    // val     wr = Output(UInt(4.W))
    val     wr = Output(UInt(1.W))
    val     size = Output(UInt(2.W))
    val     addr = Output(UInt(32.W))
    val     wdata = Output(UInt(32.W))
    val     addr_pending = Output(UInt(1.W))
    })
    def get_size( width:UInt):UInt=
        MuxLookup(width,0.U(2.W),Seq(
            0.U -> "b11".U,
            1.U -> "b00".U,
            2.U -> "b01".U,
            3.U -> "b10".U
        ))
    
    def get_data(data:UInt,offset:UInt,width:UInt):UInt = //感觉有问题，可以根据cpu书160页改!！！！
        MuxLookup(Cat(offset,width),0.U,Seq(
            "b00_01".U -> Cat(0.U(24.W),data(7,0)),
            "b00_10".U -> Cat(0.U(16.W),data(15,0)),
            "b00_11".U -> data,
            "b01_01".U -> Cat(0.U(16.W),data(7,0),0.U(8.W)),
            // "b01_10".U -> Cat(0.U(8.W),data(15,0),0.U(8.W)),//SH和LH只能读高两位或者低两位
            "b10_01".U -> Cat(0.U(8.W),data(7,0),0.U(16.W)),
            "b10_10".U -> Cat(data(15,0),0.U(16.W)),
            "b11_01".U -> Cat(data(7,0),0.U(24.W))
        ))
    val ra = io.PhyAddrE(1,0)//offset，实地址的后两位代表一个偏之
    io.addr_pending := 0.U
    io.wr       := io.MemWriteE
    // MuxLookup(Cat(io.MemWidthE,ra),0.U,Seq(
    //     "b0100".U -> "b0001".U,
    //     "b0101".U -> "b0010".U,
    //     "b0110".U -> "b0100".U,
    //     "b0111".U -> "b1000".U,
    //     "b1000".U -> "b0011".U,
    //     "b1010".U -> "b1100".U,//字节使能
    //     "b1100".U -> "b1111".U
    // )),0.U)
    io.size     := get_size(io.MemWidthE)
    io.addr     := io.PhyAddrE
    io.wdata    := get_data(io.WriteDataE,ra,io.MemWidthE)
    io.req      := io.en.asBool && (io.MemToRegE.asBool || io.MemWriteE.asBool)

}

// object dmemreq_test extends App{
//     (new ChiselStage).emitVerilog(new dmemreq)
// }

