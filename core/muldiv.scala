
package examples

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._

class unsigned_div extends BlackBox{

    val io = IO(new Bundle {
        val aclk = Input(Clock())
        val s_axis_divisor_tvalid  = Input(UInt(1.W))
        val s_axis_divisor_tready  = Output(UInt(1.W))
        val s_axis_divisor_tdata   = Input(UInt(32.W))
        val s_axis_dividend_tvalid = Input(UInt(1.W))
        val s_axis_dividend_tready = Output(UInt(1.W))
        val s_axis_dividend_tdata  = Input(UInt(32.W))
        val m_axis_dout_tvalid     = Output(UInt(1.W))
        val m_axis_dout_tdata      = Output(UInt(64.W))
  })

}

class signed_div extends BlackBox{

    val io = IO(new Bundle {
        val aclk = Input(Clock())
        val s_axis_divisor_tvalid  = Input(UInt(1.W))
        val s_axis_divisor_tready  = Output(UInt(1.W))
        val s_axis_divisor_tdata   = Input(UInt(32.W))
        val s_axis_dividend_tvalid = Input(UInt(1.W))
        val s_axis_dividend_tready = Output(UInt(1.W))
        val s_axis_dividend_tdata  = Input(UInt(32.W))
        val m_axis_dout_tvalid     = Output(UInt(1.W))
        val m_axis_dout_tdata      = Output(UInt(64.W))

  })

}

class muldiv extends Module with mips_macros{ //觉得除法器那一块有很多可以改的东西，但是怕改了出问题，还是不要改了吧

    val io = IO(new Bundle { //有隐式的时钟与复位，并且复位为高电平复位
        //流水线中的延迟器
        val en  = Input(UInt(1.W))
        val ctrl = Input(UInt(4.W))
       
        val in1  = Input(UInt(32.W))
        val in2  = Input(UInt(32.W))
        val hi   = Output(UInt(32.W))
        val lo   = Output(UInt(32.W))
        val pending = Output(UInt(1.W))
      // val b = Output(UInt(1.W))
        //val a = Output(UInt)
    })

    // val pending_Reg = RegInit(0.U(1.W))
    val counter_Reg = RegInit(0.U(1.W))
    val last_counter_Reg = RegInit(0.U(1.W))
    last_counter_Reg := counter_Reg
    // io.pending     := pending_Reg

    val mulu_answer = Wire(UInt(64.W))
    val mul_answer = Wire(UInt(64.W))

    // val hi_r = RegInit(0.U(32.W)) // 由于除法器用的ip核为时序电路不是组合逻辑,为了优化
    // val lo_r = RegInit(0.U(32.W))//用来存储除法器输出的数据

    val in1_valid_u = RegInit(0.U(1.W))
    val in2_valid_u = RegInit(0.U(1.W))
    val in1_valid = RegInit(0.U(1.W))//RegInit(0.U(1.W))
    val in2_valid = RegInit(0.U(1.W))
    val divu_output_valid =  Wire(UInt(1.W))
    val div_output_valid =  Wire(UInt(1.W))

    val in2_ready_u =Wire(UInt(1.W))
    val in1_ready_u =Wire(UInt(1.W))
    val in2_ready =Wire(UInt(1.W))
    val in1_ready =Wire(UInt(1.W))

    val divisor_Reg = Wire(UInt(32.W))
   // val divisor_Reg = RegInit(0.U(32.W))//除数
    val dividend_Reg = Wire(UInt(32.W))
   // val dividend_Reg = RegInit(0.U(32.W))//被除数


    divisor_Reg := Mux(io.en.asBool,io.in2,0.U)//这地方写的我就是沙比，如果真的是寄存器的 话
    dividend_Reg := Mux(io.en.asBool,io.in1,0.U)//这地方写的我就是沙比

    val divu_answer  = Wire(UInt(64.W))
    val div_answer  = Wire(UInt(64.W))

    in1_valid := 1.U
    in2_valid := 1.U
    in1_valid_u := 1.U
    in2_valid_u := 1.U
   // pending_Reg := 0.U
    
 
    //搞不出来能用的时序的除法器，怪的一批，到时候再看看咋办吧呜呜呜
    //val div_en_tag = io.en === 1.U && pending_Reg === 0.U //代表现实处于除法可以计算的状态
    ///val div_end_tag = pending_Reg.asBool //&& in1_ready.asBool && in2_ready.asBool
    //val divu_end_tag = pending_Reg.asBool //&& in1_ready_u.asBool && in2_ready_u.asBool
    val output_valid_tag = divu_output_valid.asBool || div_output_valid.asBool


    val a = RegInit(0.U(32.W))
    val b = RegInit(0.U(1.W))
    val limit = Wire(UInt(32.W))
    limit := Mux(io.ctrl(0),34.U,32.U)
    a := Mux(counter_Reg.asBool,Mux(a === limit,0.U,(a+1.U)),0.U)
    b := Mux(a === limit,1.U,0.U)

   // io.b := b
    when(io.en.asBool && !counter_Reg.asBool) {
        when(io.ctrl(0) ) {
            // divisor_Reg := io.in2
            // dividend_Reg := io.in1
            when(in1_ready.asBool) {
                counter_Reg := 1.U
            }
            // in1_valid := 1.U
            // in2_valid := 1.U
            // pending_Reg := 1.U
        }.elsewhen(io.ctrl(1)){ 
            // divisor_Reg := io.in2
            // dividend_Reg := io.in1
            when(in1_ready_u.asBool) {
                // pending_Reg := 1.U
                counter_Reg:= 1.U
            }
            // in1_valid_u := 1.U
            // in2_valid_u := 1.U
            // pending_Reg := 1.U
        }
    }.otherwise{
        // when(pending_Reg.asBool && in1_ready.asBool && in2_ready.asBool) {
        //     in1_valid := 0.U
        //     in2_valid := 0.U
        // }
        // when(pending_Reg.asBool && in1_ready_u.asBool && in2_ready_u.asBool) {
        //     in1_valid_u := 0.U
        //     in2_valid_u := 0.U
        // }
        when(b.asBool) {
            // pending_Reg := 0.U
            counter_Reg := 0.U
            // in1_valid := 0.U
            // in2_valid := 0.U
            // in1_valid_u := 0.U
            // in2_valid_u := 0.U

        }

    }
    io.pending := Mux(io.en.asBool && (io.ctrl(0) || io.ctrl(1) ),Mux(last_counter_Reg === 1.U && counter_Reg === 0.U,0.U,1.U),0.U)


    // divisor_Reg  := Mux(div_en_tag,io.in2,divisor_Reg ) // 二选一多路器 
    // dividend_Reg := Mux(div_en_tag,io.in1,dividend_Reg ) // 二选一多路器

    // in1_valid := MuxCase(0.U,Seq(
    //     (io.ctrl(0)  && div_en_tag && in1_ready.asBool) -> 1.U,
    //     in1_valid.asBool -> 0.U
    //     // in1_ready.asBool -> 0.U
    // )) 
    // in2_valid := MuxCase(0.U,Seq(
    //     (io.ctrl(0)  && div_en_tag&& in2_ready.asBool) -> 1.U,
    //     in2_valid.asBool -> 0.U
    // )) 

    // in1_valid_u := MuxCase(in1_valid_u,Seq(
    //     (io.ctrl(1)  && div_en_tag) -> 1.U,
    //     in1_ready_u.asBool -> 0.U
    // )) 
    // in2_valid_u := MuxCase(in1_valid_u,Seq(
    //     (io.ctrl(1)  && div_en_tag) -> 1.U,
    //     in1_ready_u.asBool -> 0.U
    // )) 

    // pending_Reg := MuxCase(pending_Reg,Seq(
    //     (io.en === 0.U) -> 0.U,
    //     //(div_en_tag &&( io.ctrl(0) === 1.U || io.ctrl(1) === 1.U)) -> 1.U,
    //     ( ( in1_valid) === 1.U ||( in1_valid_u) === 1.U ) -> 1.U,
    //     (divu_output_valid.asBool || div_output_valid.asBool) ->  0.U
    // )) 

    io.lo := Mux1H(Seq(
        io.ctrl(0) -> div_answer(63,32),
        io.ctrl(1) -> divu_answer(63,32),
        io.ctrl(2) -> mul_answer(31,0),
        io.ctrl(3) -> mulu_answer(31,0),
    ))

    io.hi := Mux1H(Seq(
        io.ctrl(0) -> div_answer(31,0),
        io.ctrl(1) -> divu_answer(31,0),
        io.ctrl(2) -> mul_answer(63,32),
        io.ctrl(3) -> mulu_answer(63,32),
    ))


    val _udiv = Module(new unsigned_div)  //时钟信号最好保持均为上升沿触发
    _udiv.io.aclk := clock//反向的时钟来驱动触除法器，以保证下一周期需要的时候肯定能有结果，并且输入也可以使用寄存器
    _udiv.io.s_axis_divisor_tvalid :=  in2_valid_u //I2为除数
    _udiv.io.s_axis_divisor_tdata  :=  divisor_Reg
    _udiv.io.s_axis_dividend_tvalid :=  in1_valid_u 
    _udiv.io.s_axis_dividend_tdata :=  dividend_Reg
    in2_ready_u :=  _udiv.io.s_axis_divisor_tready
    in1_ready_u :=  _udiv.io.s_axis_dividend_tready
    divu_answer :=  _udiv.io.m_axis_dout_tdata
    divu_output_valid :=  _udiv.io.m_axis_dout_tvalid


    val _div  = Module(new signed_div)    
    _div.io.aclk := clock
    _div.io.s_axis_divisor_tvalid :=  in2_valid //I2为除数
    _div.io.s_axis_divisor_tdata  :=  divisor_Reg
    _div.io.s_axis_dividend_tvalid :=  in1_valid 
    _div.io.s_axis_dividend_tdata :=  dividend_Reg
    in2_ready :=  _div.io.s_axis_divisor_tready
    in1_ready :=  _div.io.s_axis_dividend_tready
    div_answer :=  _div.io.m_axis_dout_tdata
    div_output_valid :=  _div.io.m_axis_dout_tvalid

    mulu_answer := io.in1 * io.in2
    mul_answer := (io.in1.asSInt * io.in2.asSInt).asUInt

  
}

object muldiv_test extends App{
    (new ChiselStage).emitVerilog(new muldiv )
}

