package examples

import chisel3._
import chisel3.stage._
import chisel3.util._
class ex_in_and_out_port extends Bundle {
    val    RegWriteE= Output(UInt(1.W))
    val    MemToRegE= Output(UInt(1.W))
    val    MemWriteE= Output(UInt(1.W))
    val    ALUCtrlE= Output(UInt(24.W))
    val    ALUSrcE= Output(UInt(2.W))
    val    RegDstE= Output(UInt(2.W))
    val    LinkE= Output(UInt(1.W))
    val    PCPlus8E= Output(UInt(32.W))
    val    LoadUnsignedE= Output(UInt(1.W))
    val    MemWidthE= Output(UInt(2.W))
    val    HiLoWriteE= Output(UInt(2.W))
    val    HiLoToRegE= Output(UInt(2.W))
    val    CP0WriteE= Output(UInt(1.W))

    val    WriteCP0AddrE= Output(UInt(5.W))
    val    WriteCP0SelE= Output(UInt(3.W))
    val    ReadCP0AddrE= Output(UInt(5.W))
    val    ReadCP0SelE= Output(UInt(3.W))
    val    PCE= Output(UInt(32.W))
    val    InDelaySlotE= Output(UInt(1.W))
    val    MemRLE=      Output(UInt(2.W))

    val    BranchJump_JrE = Output(UInt(2.W))
    
   
    // val    RtE  =       Output(UInt(32.W))
    
   
}

class id2ex extends Module with mips_macros{ //觉得除法器那一块有很多可以改的东西，但是怕改了出问题，还是不要改了吧
    val io1 = (IO(Flipped(new decoder_port)))
    val io2 = (IO(new ex_in_and_out_port))
    
    val io = IO(new Bundle { //有隐式的时钟与复位，并且复位为高电平复位
        //流水线中的延迟器
    val               en= Input(UInt(1.W))
    val               clr= Input(UInt(1.W))
    
    val         CP0ToRegE_Out= Output(UInt(1.W))
    // val          RegWriteD= Input(UInt(1.W))
    // val          MemToRegD= Input(UInt(1.W))
    // val          MemWriteD= Input(UInt(1.W))
    // val          ALUCtrlD= Input(UInt(24.W))
    // val          ALUSrcD= Input(UInt(2.W))
    // val          RegDstD= Input(UInt(2.W))
    val         RD1D= Input(UInt(32.W))
    val         RD2D= Input(UInt(32.W))
    val         RsD= Input(UInt(5.W))
    val         RtD= Input(UInt(5.W))
    val         RdD= Input(UInt(5.W))
    val         ImmD= Input(UInt(32.W))
    // val        LinkD= Input(UInt(1.W))
    val         PCPlus8D= Input(UInt(32.W))
    // val        LoadUnsignedD= Input(UInt(1.W))
    // val         MemWidthD= Input(UInt(2.W)) 
    // val         HiLoWriteD= Input(UInt(2.W))
    // val         HiLoToRegD= Input(UInt(2.W))
    // val         CP0WriteD= Input(UInt(1.W))
    // val         CP0ToRegD= Input(UInt(1.W))
    val         WriteCP0AddrD= Input(UInt(5.W))
    val         WriteCP0SelD= Input(UInt(3.W))
    val         ReadCP0AddrD= Input(UInt(5.W))
    val         ReadCP0SelD= Input(UInt(3.W))
    val         PCD = Input(UInt(32.W))
    val         InDelaySlotD= Input(UInt(1.W))
    val         ExceptionTypeD= Input(UInt(32.W))

    
    val    BranchJump_JrD = Input(UInt(2.W))
    val    BadVaddrD = Input(UInt(32.W))

    
    val         RD1E= Output(UInt(32.W))
    val         RD2E= Output(UInt(32.W))
    val         RsE= Output(UInt(5.W))
    val         RtE= Output(UInt(5.W))
    val         RdE= Output(UInt(5.W))
    val         ImmE= Output(UInt(32.W))
     val    BadVaddrE = Output(UInt(32.W))
    // val    RegWriteE= Output(UInt(1.W))
    // val    MemToRegE= Output(UInt(1.W))
    // val    MemWriteE= Output(UInt(1.W))
    // val    ALUCtrlE= Output(UInt(24.W))
    // val    ALUSrcE= Output(UInt(2.W))
    // val    RegDstE= Output(UInt(2.W))
    // val    RD1E= Output(UInt(32.W))
    // val    RD2E= Output(UInt(32.W))
    // val    RsE= Output(UInt(5.W))
    // val    RtE= Output(UInt(5.W))
    // val    RdE= Output(UInt(5.W))
    // val    ImmE= Output(UInt(32.W))
    // val    LinkE= Output(UInt(1.W))
    // val    PCPlus8E= Output(UInt(32.W))
    // val    LoadUnsignedE= Output(UInt(1.W))
    // val    MemWidthE= Output(UInt(2.W))
    // val    HiLoWriteE= Output(UInt(2.W))
    // val    HiLoToRegE= Output(UInt(2.W))
    // val    CP0WriteE= Output(UInt(1.W))
    // val    CP0ToRegE_Out= Output(UInt(1.W))
    // val    WriteCP0AddrE= Output(UInt(5.W))
    // val    WriteCP0SelE= Output(UInt(3.W))
    // val    ReadCP0AddrE= Output(UInt(5.W))
    // val    ReadCP0SelE= Output(UInt(3.W))
    // val    PCE= Output(UInt(32.W))
    // val    InDelaySlotE= Output(UInt(1.W))
    val         ExceptionTypeE_Out = Output(UInt(32.W))
    
    })
    val    RegWriteE_Reg = RegInit(0.U(1.W))
    val    MemToRegE_Reg = RegInit(0.U(1.W))
    val    MemWriteE_Reg = RegInit(0.U(1.W))
    val    ALUCtrlE_Reg = RegInit(0.U(24.W))
    val    ALUSrcE_Reg = RegInit(0.U(2.W))
    val    RegDstE_Reg = RegInit(0.U(2.W))
    val    RD1E_Reg = RegInit(0.U(32.W))
    val    RD2E_Reg = RegInit(0.U(32.W))
    val    RsE_Reg = RegInit(0.U(5.W))
    val    RtE_Reg = RegInit(0.U(5.W))
    val    RdE_Reg = RegInit(0.U(5.W))
    val    ImmE_Reg = RegInit(0.U(32.W))
    val    LinkE_Reg = RegInit(0.U(1.W))
    val    PCPlus8E_Reg = RegInit(0.U(32.W))
    val    LoadUnsignedE_Reg = RegInit(0.U(1.W))
    val    MemWidthE_Reg = RegInit(0.U(2.W))
    val    HiLoWriteE_Reg = RegInit(0.U(2.W))
    val    HiLoToRegE_Reg = RegInit(0.U(2.W))
    val    CP0WriteE_Reg = RegInit(0.U(1.W))
    val    CP0ToRegE_Reg = RegInit(0.U(1.W))
    val    WriteCP0AddrE_Reg = RegInit(0.U(5.W))
    val    WriteCP0SelE_Reg = RegInit(0.U(3.W))
    val    ReadCP0AddrE_Reg = RegInit(0.U(5.W))
    val    ReadCP0SelE_Reg = RegInit(0.U(3.W))
    val    PCE_Reg = RegInit(0.U(32.W))
    val    InDelaySlotE_Reg = RegInit(0.U(1.W))
    val    ExceptionTypeE_Reg = RegInit(0.U(32.W))
    val    MemRLE_Reg       = RegInit(0.U(2.W))
    val    BranchJump_JrE_Reg = RegInit(0.U(2.W))
    val    BadVaddrE_Reg = RegInit(0.U(32.W))


    RegWriteE_Reg           :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io1.RegWriteD,RegWriteE_Reg))
    MemToRegE_Reg           :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io1.MemToRegD,MemToRegE_Reg))
    MemWriteE_Reg           :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io1.MemWriteD,MemWriteE_Reg))
    ALUCtrlE_Reg            :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io1.ALUCtrlD,ALUCtrlE_Reg))
    ALUSrcE_Reg             :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io1.ALUSrcD,ALUSrcE_Reg))
    RegDstE_Reg             :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io1.RegDstD,RegDstE_Reg))
    RD1E_Reg                :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io.RD1D,RD1E_Reg))
    RD2E_Reg                :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io.RD2D,RD2E_Reg))
    RsE_Reg                 :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io.RsD,RsE_Reg))
    RtE_Reg                 :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io.RtD,RtE_Reg))
    RdE_Reg                 :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io.RdD,RdE_Reg))
    ImmE_Reg                :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io.ImmD,ImmE_Reg))
    LinkE_Reg               :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io1.LinkD,LinkE_Reg))
    PCPlus8E_Reg            :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io.PCPlus8D,PCPlus8E_Reg))
    LoadUnsignedE_Reg       :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io1.LoadUnsignedD,LoadUnsignedE_Reg))
    MemWidthE_Reg           :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io1.MemWidthD ,MemWidthE_Reg))
    HiLoWriteE_Reg          :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io1.HiLoWriteD,HiLoWriteE_Reg))
    HiLoToRegE_Reg          :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io1.HiLoToRegD,HiLoToRegE_Reg))
    CP0WriteE_Reg           :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io1.CP0WriteD,CP0WriteE_Reg))
    CP0ToRegE_Reg           :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io1.CP0ToRegD,CP0ToRegE_Reg))
    WriteCP0AddrE_Reg       :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io.WriteCP0AddrD,WriteCP0AddrE_Reg))
    WriteCP0SelE_Reg        :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io.WriteCP0SelD,WriteCP0SelE_Reg))
    ReadCP0AddrE_Reg        :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io.ReadCP0AddrD,ReadCP0AddrE_Reg))
    ReadCP0SelE_Reg         :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io.ReadCP0SelD,ReadCP0SelE_Reg))
    PCE_Reg                 :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io.PCD,PCE_Reg))
    InDelaySlotE_Reg        :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io.InDelaySlotD,InDelaySlotE_Reg))
    ExceptionTypeE_Reg      :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io.ExceptionTypeD,ExceptionTypeE_Reg))
    MemRLE_Reg              :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io1.MemRLD,MemRLE_Reg))
    BranchJump_JrE_Reg         :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io.BranchJump_JrD, BranchJump_JrE_Reg))
    BadVaddrE_Reg           :=        Mux(io.clr.asBool,0.U,Mux(io.en.asBool,io.BadVaddrD, BadVaddrE_Reg))    

    io2.RegWriteE               := RegWriteE_Reg 
    io2.MemToRegE               := MemToRegE_Reg 
    io2.MemWriteE     := MemWriteE_Reg 
    io2.ALUCtrlE      := ALUCtrlE_Reg 
    io2.ALUSrcE       := ALUSrcE_Reg 
    io2.RegDstE       := RegDstE_Reg 
    io.RD1E          := RD1E_Reg 
    io.RD2E          := RD2E_Reg 
    io.RsE           := RsE_Reg 
    io.RtE           := RtE_Reg 
    io.RdE           := RdE_Reg 
    io.ImmE          := ImmE_Reg 
    io2.LinkE         := LinkE_Reg 
    io2.PCPlus8E      := PCPlus8E_Reg 
    io2.LoadUnsignedE := LoadUnsignedE_Reg 
    io2.MemWidthE     := MemWidthE_Reg 
    io2.HiLoWriteE    := HiLoWriteE_Reg 
    io2.HiLoToRegE    := HiLoToRegE_Reg 
    io2.CP0WriteE     := CP0WriteE_Reg 
    io.CP0ToRegE_Out     := CP0ToRegE_Reg 
    io2.WriteCP0AddrE := WriteCP0AddrE_Reg 
    io2.WriteCP0SelE  := WriteCP0SelE_Reg 
    io2.ReadCP0AddrE  := ReadCP0AddrE_Reg 
    io2.ReadCP0SelE   := ReadCP0SelE_Reg 
    io2.PCE           := PCE_Reg 
    io2.InDelaySlotE  := InDelaySlotE_Reg 
    io2.MemRLE        := MemRLE_Reg
    io.ExceptionTypeE_Out:= ExceptionTypeE_Reg 
    io2.BranchJump_JrE := BranchJump_JrE_Reg 
    io.BadVaddrE  := BadVaddrE_Reg 
}

// object id2ex_test extends App{
//     (new ChiselStage).emitVerilog(new id2ex)
// }

