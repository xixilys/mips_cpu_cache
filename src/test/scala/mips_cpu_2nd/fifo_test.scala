//package mips_cpu_2nd
//
//import org.scalatest.freespec.AnyFreeSpec
//import chisel3._
//import chiseltest._
//import org.scalatest.freespec.AnyFreeSpec
//import chisel3.experimental.BundleLiterals._
//
//import scala.collection.mutable
//
//
////首先先测试输入输出均为2的时候的状态
//class fifo_test  extends AnyFreeSpec with ChiselScalatestTester {
//
//    //huangxinzedashabi
//
//
//    "DUt pass is hxz shabi" in {
//        test(new fifo(1024, 32, 3, 2)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
//            //        val testValues = for{pc <- 0xfc00000 to 0xfc04400 by 4} yield (pc)
//
//            val q1 = new mutable.Queue[Int]
//            for (i <- 0 until 9999) {
//                var writenum = (Math.random * 3).toInt + 1
//                var readnum = (Math.random * 2).toInt + 1
//                dut.io.read_en.poke((0).U)
//                dut.io.write_en.poke(0.U)
//                println(i)
//
//
//                dut.io.write_en.poke((writenum).U)
//                println("writenum  : " + writenum)
//                for (j <- 0 until 3) {
//                    if (j < writenum) {
//                        var in = (Math.random * 16).toInt + 1
//
//                        println("Input :" + in)
//                        q1 += in
//                        dut.io.write_in(j).poke(in.U)
//                    }
//                    else {
//                        dut.io.write_in(j).poke(0.U)
//                    }
//                }
//                if (readnum > q1.size) {
//                    readnum = 0;
//                }
//                dut.io.read_en.poke((readnum).U)
//                println("readnum : " + readnum)
//                for (j <- 0 until readnum) {
//                    println("ouput is: " + dut.io.read_out(j).peek().toString)
//                    var out = q1.dequeue()
//                    println("Expe ouput is: " + out)
//                    println("Size : " + q1.size)
////                    if (out.U != dut.io.read_out(j).peek()) {
////                        println(q1.size)
////                        while(0<1)
////                            {
////                                wait(100)
////                            }
////                    }
//                    dut.io.read_out(j).expect(out)
//                    //                    simufifo.
//                }
//
//                dut.clock.step()
//            }
//        }
//    }
//}
