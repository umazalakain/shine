package HighLevelCombinators

import Core._
import MidLevelCombinators.MapWorkgroupI
import apart.arithmetic.ArithExpr

case class MapWorkgroup(n: ArithExpr,
                        dt1: DataType,
                        dt2: DataType,
                        f: Phrase[ExpType -> ExpType],
                        array: Phrase[ExpType])
  extends AbstractMap(n, dt1, dt2, f, array, MapWorkgroup, MapWorkgroupI)