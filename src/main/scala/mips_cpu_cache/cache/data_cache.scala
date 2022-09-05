package examples

import chisel3._
import chisel3.stage._
import chisel3.util._


class data_cache  extends Module with mips_macros {
        //完全没用到chisel真正好的地方，我是废物呜呜呜呜
    val io = IO(new Bundle {
       
        val port = new axi_ram_port
        
        val stage2_stall = Output(Bool())
        val cp0_asid = Input(UInt(8.W))
        val stage1_wr = Output(Bool())

        val v_addr_for_tlb = Output(UInt(32.W))
        val p_addr_for_tlb = Input(UInt(32.W))
        //发起tlb查找请求
        //部分地址需要进行mmu 映射
        val tlb_req        = Output(Bool())
        val tlb_exception  = Input(UInt(3.W))
        //stage1就需要把产生例外的信息扔到内核里面去
        val stage1_tlb_exception = Output(UInt(3.W))
        val data_wstrb = Input(UInt(4.W))

    })

    val work_state = RegInit("b11000".U(5.W))
    val access_work_state = Wire(UInt(5.W))
    val write_counter  = RegInit(0.U(3.W))
    val read_counter  = RegInit(0.U(3.W))
    val wait_data  = RegInit(0.U(32.W))


    val dcache_tag_0 = Module(new dcache_tag).io
    val dcache_tag_1 = Module(new dcache_tag).io

    val lru = RegInit(VecInit(Seq.fill(128)(0.U(1.W))))
    val way0_dirty = RegInit(VecInit(Seq.fill(128)(0.U(1.W))))
    val way1_dirty = RegInit(VecInit(Seq.fill(128)(0.U(1.W))))
    
    val way0_wen =  Wire(Vec(8,UInt(1.W)))
    val way1_wen =  Wire(Vec(8,UInt(1.W)))
    val stage1_sram_addr_reg = RegInit(0.U(32.W))
    val stage1_sram_cache_reg = RegInit(0.U(1.W))
    val stage1_sram_wdata_reg = RegInit(0.U(32.W))
    val stage1_sram_size_reg = RegInit(0.U(2.W))
    val stage1_sram_wr_reg = RegInit(0.U(1.W))
    val stage1_sram_req_reg = RegInit(0.U.asBool)

    val stage1_sram_hit0_reg  = RegInit(0.U.asBool)
    val stage1_sram_hit1_reg  = RegInit(0.U.asBool)
    val stage1_sram_valid0_reg  = RegInit(0.U.asBool)
    val stage1_sram_valid1_reg  = RegInit(0.U.asBool)
    val stage1_wstrb_reg = RegInit(0.U(4.W))
    
    val stage1_sram_phy_addr_reg = RegInit(0.U(32.W))
    val stage1_exception  =  RegInit(0.U(3.W))
    // val stage1_sram_hit_reg  = RegInit(0.U.asBool)
    // val stage1_sram_hit_reg  = RegInit(0.U.asBool)
    // val sram_req_reg  = RegInit(0.U(1.W))
    // val access_cache_addr =  Mux(io.port.sram_req.asBool,io.port.sram_addr,sram_addr_reg)
    // val access_cache_size =  Mux(io.port.sram_req.asBool,io.port.sram_size,sram_size_reg)
    // val access_cache_wdata =  Mux(io.port.sram_req.asBool,io.port.sram_wdata,sram_wdata_reg)
    // val access_cache_wr =  Mux(io.port.sram_req.asBool,io.port.sram_wr,sram_wr_reg)    
    // val access_cache_state =  Mux(io.port.sram_req.asBool,io.port.sram_cache,sram_cache_reg)        
    val dirty_victim = Wire(UInt(1.W))
    val dcache_data_way0 =  VecInit(Seq.fill(8)(Module(new dcache_data).io))
    val dcache_data_way1 =  VecInit(Seq.fill(8)(Module(new dcache_data).io))
    val hit_0_reg       = RegInit(0.U(1.W))
    val hit_1_reg       = RegInit(0.U(1.W))

    //val state_reset = "b00000".U;
    // val state_miss_write_write_1 = "b10000".U
    // val state_miss_write_write_2 = "b10001".U
    // val state_miss_access_ram_read_2 = "b10010".U
    // val state_miss_access_ram_read_3 = "b10011".U
    // val state_miss_write_update = "b10100".U


    val state_miss_access_ram_read_3 = "b00000".U
    val state_access_ram_read_0      = "b00001".U;
    val state_access_ram_read_1      = "b00010".U;
    val state_access_ram_write_0     = "b00011".U;
    val state_access_ram_write_1     = "b00100".U;
    val state_access_ram_write_2     = "b00101".U;

    val state_miss_write_write_1     = "b00110".U
    val state_miss_write_write_2     = "b00111".U
    val state_miss_access_ram_read_2 = "b01000".U

    val state_miss_write_read_0      = "b01001".U;
    val state_miss_write_read_1      = "b01010".U;
    val state_miss_write_read_2      = "b01011".U;
    val state_miss_access_ram_read_0 = "b01100".U;
    val state_miss_access_ram_read_1 = "b01101".U;
    val state_miss_read_update       = "b01110".U;
    val state_miss_write_write_0     = "b01111".U
    val state_miss_write_update      = "b10000".U


    val state_data_ready = "b11000".U;
    val state_lookup = "b11001".U;

    val state_cache_all_invalid = "b10001".U
    val all_invalid_counter = RegInit(0.U(7.W))   


    val stage2_stall = Wire(Bool())
    val stage2_flush = 0.U.asBool
    io.stage2_stall := stage2_stall

    val stage2_addr_same_as_stage1 = Wire(Bool())
    val stage2_exception  =  RegInit(0.U(3.W))
    val stage2_wdata_reg = RegEnable(Mux(stage2_flush,0.U,stage1_sram_wdata_reg),0.U,stage2_stall)
        //RegInit(0.U(32.W))

    val state_ready_lookup_will_to_be  = Wire(UInt(5.W))
    val state_lookup_for_less_delay = Wire(UInt(5.W))

    val stage2_sram_write_reg = RegInit(0.U.asBool)

    val stage2_sram_cache_reg = RegInit(0.U.asBool)
        


    val stage1_addr_req_not_same = RegInit(0.U.asBool)
    val stage1_stall_reg = RegInit(0.U.asBool)
    stage1_stall_reg := io.port.sram_req.asBool //将req信号延迟一个周期，以取

    val write_access_complete_reg = RegInit(0.U.asBool)

    stage1_addr_req_not_same := stage1_sram_addr_reg =/= io.port.sram_addr

    stage1_sram_addr_reg := Mux(io.port.sram_req.asBool,io.port.sram_addr,stage1_sram_addr_reg)
    stage1_sram_cache_reg := Mux(io.port.sram_req.asBool,io.port.sram_cache,stage1_sram_cache_reg)
    stage1_sram_wdata_reg := Mux(io.port.sram_req.asBool,io.port.sram_wdata,stage1_sram_wdata_reg)
    stage1_sram_size_reg  := Mux(io.port.sram_req.asBool,io.port.sram_size,stage1_sram_size_reg)
    stage1_sram_wr_reg    := Mux(io.port.sram_req.asBool,io.port.sram_wr,stage1_sram_wr_reg)
    stage1_sram_req_reg   := Mux(io.port.sram_req.asBool,1.U,Mux(stage2_stall,0.U,stage1_sram_req_reg))
    stage1_wstrb_reg      := Mux(io.port.sram_req.asBool,io.data_wstrb,stage1_wstrb_reg)

    stage1_sram_hit0_reg := Mux(io.port.sram_req.asBool,dcache_tag_0.hit,stage1_sram_hit0_reg)
    stage1_sram_hit1_reg := Mux(io.port.sram_req.asBool,dcache_tag_1.hit,stage1_sram_hit1_reg)
    stage1_sram_valid0_reg := Mux(io.port.sram_req.asBool,dcache_tag_0.valid,stage1_sram_valid0_reg)
    stage1_sram_valid1_reg := Mux(io.port.sram_req.asBool,dcache_tag_1.valid,stage1_sram_valid1_reg)
    // sram_req_reg := io.port.sram_req
//简单起见，将后面的东西
    way0_dirty(stage1_sram_addr_reg(11,5)) := Mux(state_lookup_for_less_delay === state_lookup && stage1_sram_wr_reg.asBool && stage1_sram_hit0_reg && stage1_sram_valid0_reg,1.U,
        Mux(work_state === state_miss_read_update && lru(stage1_sram_addr_reg(11,5)) === 0.U,0.U,
        Mux(work_state === state_miss_write_update &&lru(stage1_sram_addr_reg(11,5)) === 0.U,1.U,way0_dirty(stage1_sram_addr_reg(11,5)))))//告诉我有没有脏数据，只需要在这些情况进行更新

    way1_dirty(stage1_sram_addr_reg(11,5)) := Mux(state_lookup_for_less_delay === state_lookup && stage1_sram_wr_reg.asBool && stage1_sram_hit1_reg && stage1_sram_valid1_reg,1.U,
        Mux(work_state === state_miss_read_update && lru(stage1_sram_addr_reg(11,5)) === 1.U,0.U,
        Mux(work_state === state_miss_write_update &&lru(stage1_sram_addr_reg(11,5)) === 1.U,1.U,way1_dirty(stage1_sram_addr_reg(11,5)))))


    // val dirty_victim_addr = stage1_sram_addr_reg//Mux(io.port.sram_req.asBool,io.port.sram_addr,sram_addr_reg)
    dirty_victim := Mux(lru(stage1_sram_addr_reg(11,5)) === 0.U,way0_dirty(stage1_sram_addr_reg(11,5)),way1_dirty(stage1_sram_addr_reg(11,5)))//index

    dcache_tag_0.op  := 0.U
    dcache_tag_1.op  := 0.U
    dcache_tag_0.waddr := stage1_sram_addr_reg
    dcache_tag_1.waddr := stage1_sram_addr_reg

    dcache_tag_0.raddr := io.port.sram_addr
    dcache_tag_1.raddr := io.port.sram_addr

    val stage1_addr_line_mapping = memory_mapping(stage1_sram_addr_reg)

    io.tlb_req := check_mapped(stage1_sram_addr_reg) && stage1_sram_req_reg
    io.v_addr_for_tlb := stage1_sram_addr_reg
    io.stage1_wr      := stage1_sram_wr_reg
    stage1_sram_phy_addr_reg  := Mux(stage1_stall_reg,memory_mapping(Mux(io.tlb_req,io.p_addr_for_tlb,stage1_sram_addr_reg)),stage1_sram_phy_addr_reg)
    stage1_exception := Mux(stage2_stall,0.U,io.tlb_exception)

    
    lru(stage1_sram_addr_reg(11,5)) := Mux(state_lookup_for_less_delay === state_lookup,
        Mux(stage1_sram_hit0_reg && stage1_sram_valid0_reg,1.U.asBool,
        Mux(stage1_sram_hit1_reg &&stage1_sram_valid1_reg,0.U.asBool,lru(stage1_sram_addr_reg(11,5)) )),
        Mux(work_state === state_miss_read_update||work_state === state_miss_write_update,~lru(stage1_sram_addr_reg(11,5)),  lru(stage1_sram_addr_reg(11,5))))
    
    val hit = (stage1_sram_hit0_reg && stage1_sram_valid0_reg) ||
        (stage1_sram_hit1_reg && stage1_sram_valid1_reg)
   

    state_ready_lookup_will_to_be :=  Mux(hit,Mux(stage1_sram_req_reg.asBool,Mux(stage1_sram_wr_reg.asBool,
            Mux(stage1_sram_cache_reg.asBool,state_lookup,state_access_ram_write_0),Mux(stage1_sram_cache_reg.asBool,state_lookup,state_access_ram_read_0)),state_lookup),
            Mux(stage1_sram_req_reg.asBool,Mux(!stage1_sram_cache_reg.asBool,Mux(stage1_sram_wr_reg.asBool,state_access_ram_write_0,state_access_ram_read_0),
            Mux(dirty_victim.asBool,state_miss_write_read_0,Mux(stage1_sram_wr_reg.asBool,state_miss_access_ram_read_2,state_miss_access_ram_read_0))),state_lookup))
   
    val state_ready_lookup_should_be = Mux(hit,Mux(stage1_sram_req_reg.asBool,Mux(stage1_sram_cache_reg.asBool,state_lookup,0.U),state_lookup),
            Mux(stage1_sram_req_reg.asBool,0.U,state_lookup))

  
    access_work_state := MuxLookup(work_state,work_state,Seq(
        state_access_ram_read_0 -> Mux(io.port.arready.asBool, state_access_ram_read_1,work_state),
        state_access_ram_read_1 -> Mux(io.port.rvalid.asBool,state_data_ready,work_state),
        state_data_ready        -> state_ready_lookup_will_to_be,
        state_access_ram_write_0-> Mux(io.port.awready.asBool,state_access_ram_write_1,work_state),
        state_access_ram_write_1-> Mux(io.port.wready.asBool,state_access_ram_write_2,work_state),
        state_access_ram_write_2-> state_data_ready,//Mux(io.port.bvalid.asBool || stage1_addr_req_not_same,state_data_ready,state_access_ram_write_2),
        state_lookup            -> state_ready_lookup_will_to_be,
        state_miss_access_ram_read_0 -> Mux(io.port.arready.asBool,state_miss_access_ram_read_1,work_state),
        state_miss_access_ram_read_1 -> Mux(io.port.rlast.asBool && io.port.rvalid.asBool,Mux(stage1_sram_wr_reg.asBool,state_miss_write_update,state_miss_read_update),work_state),
        state_miss_read_update    -> state_data_ready,
        state_miss_write_read_0   -> Mux(io.port.awready.asBool,state_miss_write_read_1,work_state),
        state_miss_write_read_1   -> Mux(io.port.wready.asBool && write_counter === "b111".U,state_miss_write_read_2,work_state),
        state_miss_write_read_2   -> Mux(io.port.bvalid.asBool,state_miss_access_ram_read_0,work_state),
       // state_lookup      -> Mux(hit,state_data_ready,Mux(dirty_victim.asBool,state_miss_write_write_0,state_miss_access_ram_read_2)),
        state_miss_access_ram_read_2 -> Mux(io.port.arready.asBool,state_miss_access_ram_read_3,work_state),
        state_miss_access_ram_read_3 -> Mux(io.port.rvalid.asBool  && io.port.rlast.asBool,state_miss_write_update,work_state),
        state_miss_write_update   -> state_data_ready,
        state_miss_write_write_0  -> Mux(io.port.awready.asBool,state_miss_write_write_1,work_state),
        state_miss_write_write_1  -> Mux(io.port.wready.asBool && write_counter === "b111".U , state_miss_write_write_2,work_state),
        state_miss_write_write_2  -> Mux(io.port.bvalid.asBool,state_miss_access_ram_read_2,work_state)))

      val access_work_state_for_stall = MuxLookup(work_state,work_state,Seq(
        state_access_ram_read_1 -> Mux(io.port.rvalid.asBool,state_data_ready,work_state),
        state_data_ready        -> state_ready_lookup_should_be,
        state_access_ram_write_2->  state_data_ready,//Mux(io.port.bvalid.asBool || stage1_addr_req_not_same ,state_data_ready,state_access_ram_write_2),
        state_lookup            -> state_ready_lookup_should_be,
        state_miss_read_update    -> state_data_ready,
        state_miss_write_update   -> state_data_ready))

    state_lookup_for_less_delay := MuxLookup(work_state,work_state,Seq(
        state_data_ready        -> state_ready_lookup_should_be,
        state_lookup            -> state_ready_lookup_should_be))

    stage2_stall := access_work_state_for_stall(4,3) === "b11".U || stage1_exception =/= 0.U// || access_work_state_for_stall === state_data_ready  //|| access_work_state === state_lookup_write
   //stage1 打一拍接收数据
    work_state := Mux(stage1_exception =/= 0.U,state_lookup,access_work_state)
    //axi总线写完下一刻就读可能会导致数据错误
    wait_data := Mux(work_state === state_access_ram_read_1 && io.port.rvalid.asBool,io.port.rdata,
        Mux(work_state === state_miss_access_ram_read_1 && io.port.rvalid.asBool && read_counter === stage1_sram_addr_reg(4,2),io.port.rdata,wait_data))
    
    write_counter := Mux(work_state === state_miss_write_read_1,Mux(io.port.wready.asBool,Mux(write_counter === "b111".U,0.U,write_counter+1.U),write_counter),
        Mux(work_state === state_miss_write_write_1,Mux(io.port.wready.asBool,Mux(write_counter === "b111".U,0.U,write_counter+1.U),write_counter),write_counter))
    read_counter := Mux(work_state === state_miss_access_ram_read_1,Mux(io.port.rvalid.asBool && io.port.rlast.asBool, 0.U, Mux(io.port.rvalid.asBool,read_counter+1.U,read_counter )),
        Mux(work_state === state_miss_access_ram_read_3,Mux(io.port.rvalid.asBool && io.port.rlast.asBool, 0.U, Mux(io.port.rvalid.asBool,read_counter+1.U,read_counter )),read_counter))
//受到的数据
    val hit0 = (stage1_sram_hit0_reg && stage1_sram_valid0_reg) 
   
    val hit1 = (stage1_sram_hit1_reg && stage1_sram_valid1_reg)

    
    val stage2_sram_addr_reg = RegEnable(Mux(stage2_flush,0.U,stage1_sram_addr_reg),0.U,stage2_stall) //把这个加4的过程放到stage2，尽量减少stage1的逻辑层数
    // val stage2_sram_addr_plus_reg = RegEnable(Mux(stage2_flush,0.U,stage1_sram_addr_reg + 4.U),0.U,stage2_stall) //后面依靠output_valid 信号来决定哪个信号是有效的
    // val stage2_sram_addr_plus_plus_reg = RegEnable(Mux(stage2_flush,0.U,stage1_sram_addr_reg + 8.U),0.U,stage2_stall)
    



    stage2_sram_cache_reg := Mux(stage2_flush,0.U,Mux(stage2_stall,stage1_sram_cache_reg,stage2_sram_cache_reg))   
    val stage2_sram_req_reg = RegInit(0.U.asBool)
    stage2_sram_req_reg := Mux(stage2_flush,0.U,Mux(stage2_stall,stage1_sram_req_reg,stage2_sram_req_reg))


    stage2_sram_write_reg := Mux(stage2_flush,0.U,Mux(stage2_stall,stage1_sram_wr_reg,stage2_sram_write_reg))
   // val stage2_sram_req_reg = RegEnable(Mux(stage2_flush,0.U,stage1_sram_req_reg),0.U(1.W),stage2_stall)  
    
    val stage2_hit0_reg = RegInit(0.U.asBool)
    stage2_hit0_reg := Mux(stage2_flush,0.U,Mux(stage2_stall,hit0,stage2_hit0_reg))
        
       // RegEnable(Mux(stage2_flush,0.U(1.W),hit0),0.U(1.W),stage2_stall) 
    val stage2_hit1_reg = RegInit(0.U.asBool)
    stage2_hit1_reg := Mux(stage2_flush,0.U,Mux(stage2_stall,hit1,stage2_hit1_reg))
       // RegEnable(Mux(stage2_flush,0.U(1.W),hit1),0.U(1.W),stage2_stall) 

    val stage2_hit_reg  = RegInit(0.U.asBool)
    stage2_hit_reg := Mux(stage2_flush,0.U,Mux(stage2_stall,hit,stage2_hit_reg))

    stage2_exception := Mux(stage2_stall,stage1_exception,stage2_exception)
    io.stage1_tlb_exception := stage1_exception

    //stage 3  存入指令缓冲队列，在issue阶段前仍然为顺序结构

   // val stage_2_work_state = RegInit(0.U(1.W))
   // val access_stage_2_work_state = Wire(Bool())
   // io.cache_working_on := access_stage_2_work_state
    //access_stage_2_work_state := Mux(stage2_sram_req_reg.asBool,1.U,stage_2_work_state)
   // stage_2_work_state := access_stage_2_work_state
    val word_selection0 = dcache_data_way0(stage2_sram_addr_reg(4,2)).rdata
    val word_selection1 = dcache_data_way1(stage2_sram_addr_reg(4,2)).rdata


    val hit_word = Mux(stage2_hit0_reg.asBool,word_selection0,word_selection1) //如果没有命中可以通过data_ok来判断是否需要接受数据

    // val word_selection0 = dcache_data_way0(sram_addr_reg(4,2)).rdata
    // val word_selection1 = dcache_data_way1(sram_addr_reg(4,2)).rdata

    val wb_word0 = dcache_data_way0(write_counter).rdata
    val wb_word1 = dcache_data_way1(write_counter).rdata
    val wen_way0_wire = Wire(Vec(8,(UInt(4.W))))
    val wen_way1_wire = Wire(Vec(8,(UInt(4.W))))

    val writeback_data = Mux(lru(stage1_sram_addr_reg(11,5)).asBool,wb_word1,wb_word0)
    val way0_burst_read_wen = (work_state === state_miss_access_ram_read_1 || work_state === state_miss_access_ram_read_3) && io.port.rvalid.asBool && lru(stage1_sram_addr_reg(11,5)) === 0.U
    val way1_burst_read_wen = (work_state === state_miss_access_ram_read_1 || work_state === state_miss_access_ram_read_3) && io.port.rvalid.asBool && lru(stage1_sram_addr_reg(11,5)) === 1.U
    for(i <- 0 to 7 ) {
        dcache_data_way0(i).addr := stage1_sram_addr_reg
        dcache_data_way0(i).wdata := Mux(work_state === state_miss_write_update || state_lookup_for_less_delay === state_lookup ,stage1_sram_wdata_reg,Mux(work_state === state_miss_access_ram_read_1 ||work_state === state_miss_access_ram_read_3,io.port.rdata,0.U))
        dcache_data_way0(i).en := 1.U
        dcache_data_way0(i).wen := wen_way0_wire(i)
        dcache_data_way1(i).addr := stage1_sram_addr_reg
        dcache_data_way1(i).wdata := Mux(work_state === state_miss_write_update || state_lookup_for_less_delay === state_lookup ,stage1_sram_wdata_reg,Mux(work_state === state_miss_access_ram_read_1 ||work_state === state_miss_access_ram_read_3,io.port.rdata,0.U))
        dcache_data_way1(i).en := 1.U
        dcache_data_way1(i).wen := wen_way1_wire(i)   
    }

    for(i <- 0 to 7) {
        wen_way0_wire(i) :=  Mux( stage1_sram_addr_reg(4,2) === i.asUInt && ((state_lookup_for_less_delay === state_lookup && stage1_sram_wr_reg.asBool && stage1_sram_hit0_reg && stage1_sram_valid0_reg)||
            (work_state === state_miss_write_update  && lru(stage1_sram_addr_reg(11,5)) === 0.U)),stage1_wstrb_reg,Cat(Seq.fill(4)(way0_wen(i))))
        wen_way1_wire(i) :=  Mux( stage1_sram_addr_reg(4,2) === i.asUInt && ((state_lookup_for_less_delay === state_lookup && stage1_sram_wr_reg.asBool && stage1_sram_hit1_reg && stage1_sram_valid1_reg)||
            (work_state === state_miss_write_update  && lru(stage1_sram_addr_reg(11,5)) === 1.U)),stage1_wstrb_reg,Cat(Seq.fill(4)(way1_wen(i))))
    }
    for(i <- 0 to 7) {
        way0_wen(i)  := Mux(i.asUInt === read_counter,way0_burst_read_wen ,0.U) 
        way1_wen(i)  := Mux(i.asUInt === read_counter,way1_burst_read_wen ,0.U) 
    }
 

   
    val cache_wdata = Mux(work_state === state_miss_access_ram_read_1 || work_state === state_miss_access_ram_read_3,io.port.rdata,
        Mux(work_state === state_lookup,stage1_sram_wdata_reg,0.U))
  
    
    dcache_tag_0.wen := Mux((work_state === state_miss_access_ram_read_1 ||work_state === state_miss_access_ram_read_3 ) && lru(stage1_sram_addr_reg(11,5)) === 0.U,1.U,0.U)
    dcache_tag_1.wen := Mux((work_state === state_miss_access_ram_read_1 ||work_state === state_miss_access_ram_read_3 ) && lru(stage1_sram_addr_reg(11,5)) === 1.U,1.U,0.U)//最近使用的是cache 0
    dcache_tag_0.wdata := Mux((work_state === state_miss_access_ram_read_1 ||work_state === state_miss_access_ram_read_3 ) ,Cat(1.U(1.W),stage1_sram_addr_reg(31,12)),0.U)
    dcache_tag_1.wdata := Mux((work_state === state_miss_access_ram_read_1 ||work_state === state_miss_access_ram_read_3 ) ,Cat(1.U(1.W),stage1_sram_addr_reg(31,12)),0.U)
      
    //axi signal
// r 读数据通道
    io.port.rready := 1.U
// ar 读地址通道
    io.port.arid := 1.U
    io.port.araddr := Mux(work_state === state_access_ram_read_0,Cat(stage1_sram_phy_addr_reg),
        Mux(work_state === state_miss_access_ram_read_0 || work_state === state_miss_access_ram_read_2,Cat(stage1_sram_phy_addr_reg(31,5),0.U(5.W)),0.U))
    io.port.arlen  := Mux(stage1_sram_cache_reg.asBool,"b111".U,0.U)
    io.port.arsize :=  Mux(stage1_sram_cache_reg.asBool,"b010".U,stage1_sram_size_reg)
        //     0.U -> "b000".U,
        //     1.U -> "b001".U
        // )))
    io.port.arburst := Mux(stage1_sram_cache_reg.asBool,1.U,0.U)
    io.port.arlock  := 0.U
    io.port.arcache := 0.U
    io.port.arprot  := 0.U

    stage2_addr_same_as_stage1 := stage2_sram_addr_reg === stage1_sram_addr_reg
    val stage2_write_stage1_read = stage2_sram_write_reg && !stage1_sram_wr_reg
    when(work_state === state_access_ram_write_0 ) {
        write_access_complete_reg := 1.U.asBool
    }.elsewhen(io.port.bvalid.asBool) {
        write_access_complete_reg := 0.U.asBool
    }.otherwise{
        write_access_complete_reg := write_access_complete_reg
    }
    
    val  awaddr_miss_addr = Cat(Mux(lru(stage1_sram_addr_reg(11,5)) === 0.U,dcache_tag_0.tag,dcache_tag_1.tag),stage1_sram_phy_addr_reg(11,5),0.U(5.W))

    io.port.arvalid := Mux(work_state === state_access_ram_read_0 || work_state === state_miss_access_ram_read_0 ||
        work_state === state_miss_access_ram_read_2,!(stage2_addr_same_as_stage1 && stage2_write_stage1_read && write_access_complete_reg) ,0.U).asBool && stage1_exception === 0.U
    io.port.rready  := 1.U
// aw 写地址通道
    io.port.awid    := "b0001".U
    io.port.awaddr  := MuxLookup(work_state,0.U,Seq(
        state_access_ram_write_0 -> Cat(stage1_sram_phy_addr_reg(31,0)),
        state_miss_write_read_0 ->  awaddr_miss_addr,//Cat(Mux(lru(stage1_sram_addr_reg(11,5)) === 0.U,dcache_tag_0.tag,dcache_tag_1.tag),stage1_sram_phy_addr_reg(11,5),0.U(5.W)),
        state_miss_write_write_0 -> awaddr_miss_addr//Cat(Mux(lru(stage1_sram_addr_reg(11,5)) === 0.U,dcache_tag_0.tag,dcache_tag_1.tag),stage1_sram_phy_addr_reg(11,5),0.U(5.W))//脏数据写回
    ))
    io.port.awlen   := Mux(stage1_sram_cache_reg.asBool,"b111".U,0.U)
    io.port.awsize  := Mux(stage1_sram_cache_reg.asBool,"b010".U,/*MuxLookup(*/stage1_sram_size_reg)//,"b010".U,Seq(
        //     0.U -> "b000".U,
        //     1.U -> "b001".U
        // )))
    io.port.awburst := Mux(stage1_sram_cache_reg.asBool,"b01".U,0.U)
    io.port.awlock  := "b00".U
    io.port.awcache := "b0000".U // _0握手
    io.port.awprot  := "b000".U
    io.port.awvalid := Mux(work_state === state_access_ram_write_0 || work_state ===state_miss_write_write_0 || work_state === state_miss_write_read_0,1.U,0.U).asBool && stage1_exception === 0.U
// w 写数据通道
    io.port.wid    := "b0001".U //_1 传输数据，第三个周期即可受到数据
    io.port.wdata  := Mux(work_state === state_access_ram_write_1,stage1_sram_wdata_reg,Mux(work_state === state_miss_write_read_1 ||
        work_state === state_miss_write_write_1,writeback_data ,0.U))
    io.port.wstrb  := Mux(work_state === state_access_ram_write_1,stage1_wstrb_reg,Mux(work_state === state_miss_write_read_1 ||
        work_state === state_miss_write_write_1,"b1111".U ,0.U))
    io.port.wlast  := work_state === state_access_ram_write_1 || ((work_state === state_miss_write_read_1 || work_state === state_miss_write_write_1 ) 
        && write_counter === "b111".U )
    io.port.wvalid := work_state === state_access_ram_write_1 || work_state === state_miss_write_read_1 || work_state === state_miss_write_write_1 
//  b 写应答
    io.port.bready := 1.U
//sram like
    io.port.sram_addr_ok := 1.U
    io.port.sram_data_ok := /*Mux(*/Mux(stage2_sram_req_reg.asBool,work_state === state_data_ready  || work_state === state_lookup,0.U)//&& !io.port.sram_req.asBool,1.U,Mux(((work_state === state_lookup_read && !stage1_sram_wr_reg.asBool) || (work_state === state_lookup_write && stage1_sram_wr_reg.asBool)),hit,0.U))
    val sram_rdata_reg = RegInit(0.U(32.W))
    sram_rdata_reg  := Mux(work_state === state_data_ready,wait_data,Mux(work_state === state_lookup,hit_word,sram_rdata_reg))
    val stage2_stall_reg = RegInit(0.U.asBool)
    stage2_stall_reg := stage2_stall.asBool
    io.port.sram_rdata := Mux(stage2_stall_reg,Mux(work_state === state_data_ready,wait_data,Mux(work_state === state_lookup,hit_word,0.U)),sram_rdata_reg)

}