package examples

import chisel3._
import chisel3.stage._
import chisel3.util._




class pc2if extends Module {
    // val io  =    new inst_port.     
    val io = IO(new Bundle { //有隐式的时钟与复位，并且复位为高电平复位
        
    val              en =     Input(UInt(1.W))
    val         PC_next =     Input(UInt(32.W))


    val PhyAddrP        =     Input(UInt(32.W))
    val InstUnalignedP  =     Input(UInt(1.W))
    val inst_port_test  =     new inst_port
    // val inst_addr_ok    =     Input(UInt(1.W)) //地址和数据ok
    // val inst_data_ok    =     Input(UInt(1.W))
    val ExceptionW      =     Input(UInt(1.W)) // 异常
    val ReturnPCW       =     Input(UInt(32.W))
    val inst_cache       =     Input(UInt(1.W))
   
    val PCP             =     Output(UInt(32.W)) //pc pc
    val PCF             =     Output(UInt(32.W)) //pc if
    // val inst_addr       =     Output(UInt(32.W))
    // val inst_wr         =     Output(UInt(1.W))
    // val inst_size       =     Output(UInt(2.W))
    // val inst_wdata      =     Output(UInt(32.W))
    // val inst_req        =     Output(UInt(1.W)) // 应该是请求指令 ins request
    val InstUnalignedF  =     Output(UInt(1.W))
  
    val addr_pending    =     Output(UInt(1.W)) //代办的，数据待处理
    val data_pending    =     Output(UInt(1.W))
    val InExceptionF    =     Output(UInt(1.W))
    
    })
    val return_pc_Reg   = RegInit(0.U(32.W)) //由cp0中引出，应该是表示函数该返回了
    val PCF_Reg         = Reg(UInt(32.W))
    val rst_flag     = RegInit(0.U(1.W))
    val req_wait     = RegInit(0.U(1.W))
    val req_pending  = RegInit(0.U(1.W))
    val inst_req_Reg = RegInit(0.U(1.W))
    val inst_cache_Reg = RegInit(0.U(1.W))
        // /RegInit(Cat(0xbfbf.U,0xfffc.U))
    val InstUnalignedF_Reg = RegInit(0.U(1.W))
    val inst_data_ok_Reg = RegInit(0.U(1.W))
    val access_returnpc = Mux(io.ExceptionW.asBool,io.ReturnPCW,return_pc_Reg)
    inst_data_ok_Reg := io.inst_port_test.inst_data_ok
  

    val req_miss   = io.inst_port_test.inst_data_ok.asBool ^ req_pending.asBool //不取数了
    val req_stall  = req_wait.asBool || req_miss
    val req_able = (rst_flag.asBool && io.en.asBool &&(!req_stall.asBool || (req_stall && inst_data_ok_Reg.asBool)))

    
    val req_wait_reg = RegInit(0.U(1.W))
    val access_req_wait = Mux(inst_req_Reg.asBool&& !(io.inst_port_test.inst_hit.asBool && inst_cache_Reg.asBool),1.U,Mux(io.inst_port_test.inst_data_ok.asBool,0.U,req_wait_reg))
    inst_req_Reg := io.inst_port_test.inst_req
    inst_cache_Reg := Mux(io.inst_port_test.inst_req.asBool,io.inst_cache.asBool,0.U)
    req_wait_reg := Mux(inst_req_Reg.asBool && !(io.inst_port_test.inst_hit.asBool && inst_cache_Reg.asBool),1.U,
        Mux(io.inst_port_test.inst_data_ok.asBool,0.U,req_wait_reg))
    val req_able_wire = rst_flag.asBool && io.en.asBool && (access_req_wait.asBool && io.inst_port_test.inst_data_ok.asBool || !access_req_wait.asBool)

    val has_req_data_instunalignedf_reg = RegInit(0.U(1.W))
    has_req_data_instunalignedf_reg := Mux(req_able_wire,io.InstUnalignedP,has_req_data_instunalignedf_reg )

    io.PCF              := PCF_Reg
    io.InstUnalignedF   := InstUnalignedF_Reg
    //没用寄存器为什么呢
    io.PCP := Mux(reset.asBool,0xbfc0.U<<16,Mux(io.InExceptionF.asBool,access_returnpc,io.PC_next)) //高电平复位，系统全都是高电平复位

    val in_exception_Reg = RegInit(0.U(1.W))
    io.InExceptionF     := MuxCase(in_exception_Reg,Seq(
        io.ExceptionW.asBool                -> 1.U     
    ))

    val exception_sat = (~in_exception_Reg.asBool)  && (io.ExceptionW.asBool)

    in_exception_Reg := MuxCase(in_exception_Reg,Seq(
        io.ExceptionW.asBool                -> 1.U,
        io.inst_port_test.inst_req.asBool   -> 0.U             
    ))
    return_pc_Reg    := Mux(exception_sat,io.ReturnPCW,return_pc_Reg )

 
    rst_flag        := ~(reset.asBool)
    io.addr_pending := 0.U

    io.inst_port_test.inst_req    := req_able_wire  && !io.InstUnalignedP.asBool//不复位，不使能，或者req 停止或者req 数据不ok都不会提出ins req
    io.data_pending   := (!reset.asBool && !io.inst_port_test.inst_req.asBool && !io.inst_port_test.inst_data_ok && io.en.asBool  && !has_req_data_instunalignedf_reg.asBool && access_req_wait.asBool ) //&& !io.InstUnalignedF.asBool //数据处理中，复位不说了，ins req出去了，并且正在等待

    req_pending := io.inst_port_test.inst_req
    req_wait    := Mux(req_miss.asBool, ~req_wait,req_wait) 
//下面是和pc指针有关的东西
    io.inst_port_test.inst_addr := io.PhyAddrP
    io.inst_port_test.inst_wr   := 0.U //为0是读指令
    io.inst_port_test.inst_size := 2.U // 一直都是32位，也就是2.U
    io.inst_port_test.inst_wdata := 0.U //写请求的写数据，咱们只需要读数据即可

    PCF_Reg := Mux(reset.asBool,Cat(0xbfbf.U,0xfffc.U),Mux(req_able_wire,io.PCP,PCF_Reg))
    InstUnalignedF_Reg :=  Mux(req_able_wire,io.InstUnalignedP, InstUnalignedF_Reg)


 
}

// object pc2if_test extends App{
//     (new ChiselStage).emitVerilog(new pc2if )
// }

