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
//    "DUt pass is wzy shabi" in {
//        test(new free_list(64, 8)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
//            //        val testValues = for{pc <- 0xfc00000 to 0xfc04400 by 4} yield (pc)
//            var simuque: List[Int] = List()
//
//            for (i <- 0 until 64) {
//                simuque = i :: simuque
//            }
//            simuque.reverse
//            for (i <- 0 until 64) {
//                var writenum = (Math.random * 3).toInt
//                var readnum = (Math.random * 3).toInt
//                if(64 - simuque.size < writenum)
//                    writenum = 64 - simuque.size
//
//                dut.io.read_en.poke(readnum.U)
//                dut.io.write_en.poke(writenum.U)
//
//
//                for (j <- 0 until writenum) {
//                    var randomwritenum = (Math.random * 64).toInt
//                    while (simuque.exists(s => s == randomwritenum.toInt)) {
//                        randomwritenum = (Math.random * 64).toInt/
//                    }/
//
//                    /** *********************************** */
//                    /*ADDVAL*/
//                    dut.io.write_in(j).poke(randomwritenum.U)
//                    println("ADD VAL: " + randomwritenum)
//
//                    /** *********************************** */
//                    simuque = randomwritenum :: simuque
//                }
//                for (j <- 0 until readnum) {
//                    /** *************************** */
//                    /*READ VAL ANDCHECK */
//
////                    println( )
//
//
//                    var res = dut.io.read_out(j).peekInt()
//                    //                        var res = 0;//res = read val
//                    if (simuque.exists(s => s == res)) {
//                        var resindex = simuque.indexOf(res)
//                        simuque = simuque.take(resindex) ++ simuque.drop(resindex + 1)
//                        println("CHECK SUCCESS   " + res)
//                    }
//                    else {
//                        println("==================CHECK FAIL   ============" + res)
//                    }
//
//
//                    /** *************************** */
//                }
//            }
//
//            println(simuque.sorted)
//        }
//    }
//}