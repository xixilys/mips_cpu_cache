package examples

import chisel3._
import chisel3.stage._
import chisel3.util._
import os.makeDir

class pre_cfu extends Module{
     val io = IO(new Bundle { //这两根线同时需要移到
        val stage_pc_cal_stall = Output(Bool())
       // val stage_pc_cal_flush = Output(Bool())
        val stage_fec_1_stall = Output(Bool())
        val stage_fec_1_flush = Output(Bool())
        // val stage_fec_2_stall = Output(Bool())
        // val stage_fec_2_req = Input(Bool())

        val stage2_stall = Input(Bool())
       // val stage_fec_2_flush = Output(Bool())
        
        val stage_fec_2_must_continue = Output(Bool())
        //在没有分支预测的东西里面就是
        val data_ok = Input(Bool())
        val hit     = Input(Bool())
        val branch_error          = Input(Bool())
        val fifo_full             = Input(Bool())
        val pc_check_error        = Input(Bool())
       // val inst_cache_working_on = Input(Bool())
    })
    //能够让前面这一堆东西停止的东西
    //例外，比如说指令错误例外啥的，这也是一种错误
   // val self_start = !io.inst_cache_working_on
    val inst_not_ok = !(io.hit || io.data_ok /*|| self_start*/) 
  
   // io.stage_fec_2_stall := (!(io.fifo_full || inst_not_ok))|| (!io.stage_fec_2_req && io.stage2_stall)//相当于大家一起流一个回合
   // io.stage_fec_2_flush := 0.U//io.branch_error 

    io.stage_fec_1_stall := io.branch_error || !(  io.fifo_full || inst_not_ok)// ||(!io.stage_fec_2_req && io.stage2_stall)
    io.stage_fec_1_flush := 0.U//io.branch_error

    io.stage_pc_cal_stall := io.branch_error || !(  io.fifo_full || inst_not_ok )// || (!io.stage_fec_2_req && io.stage2_stall)
    //io.stage_pc_cal_flush := io.branch_error

    io.stage_fec_2_must_continue := 0.U





  
}
