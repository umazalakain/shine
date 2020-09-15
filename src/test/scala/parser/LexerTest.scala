package parser

import OpType.{BinOpType, UnaryOpType}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.equal
import org.scalatest.matchers.should.Matchers._

class LexerTest extends  AnyFlatSpec {

  "RecognizeLexeme" should "work for the identity" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/identity.rise"
    val file: FileReader =  FileReader(fileName)
    val lexer: RecognizeLexeme = RecognizeLexeme(file)
    lexer.tokens match {
      case Backslash(span4) :: Identifier("x", span3) :: Arrow(span2) :: Identifier("x", span1) :: Nil => true
      case a => throw new Exception(a.toString())
    }
  }

  "RecognizeLexeme" should "work for the constant42" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/constant42.rise"
    val file: FileReader = FileReader(fileName)
    val lexer: RecognizeLexeme = RecognizeLexeme(file)
    lexer.tokens match {
      case Backslash(span4) :: Identifier("c", span3) :: Arrow(span2) :: I32(42, span1) :: Nil => true
      case a => throw new Exception(a.toString())
    }
  }

  "RecognizeLexeme" should "work for the longIdentity" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/longIdentity.rise"
    val file: FileReader = FileReader(fileName)
    val lexer: RecognizeLexeme = RecognizeLexeme(file)
    lexer.tokens match {
      case Backslash(span4) :: Identifier("Kevin", span3) :: Arrow(span2) :: Identifier("Kevin", span1) :: Nil => true
      case a => throw new Exception(a.toString())
    }
  }

  "RecognizeLexeme" should "work for the identityWithI32" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/identityWithI32.rise"
    val file: FileReader =  FileReader(fileName)
    val lexer: RecognizeLexeme = RecognizeLexeme(file)
    lexer.tokens match {
      case Backslash(span4) :: Identifier("x", span3) :: Colon(span0) :: Type(IntTyp(), span) :: Arrow(span2) :: Identifier("x", span1) :: Nil => true
      case a => throw new Exception(a.toString())
    }
  }

  "RecognizeLexeme" should "work for the longIdentityWithI32" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/longIdentityWithI32.rise"
    val file: FileReader =  FileReader(fileName)
    val lexer: RecognizeLexeme = RecognizeLexeme(file)
    lexer.tokens match {
      case Backslash(span4) :: Identifier("jens", span3) :: Colon(span0) :: Type(IntTyp(), span) :: Arrow(span2) :: Identifier("jens", span1) :: Nil => true
      case a => throw new Exception(a.toString())
    }
  }

  "RecognizeLexeme" should "work for the ComplexIdentifier" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/ComplexIdentifier.rise"
    val file: FileReader =  FileReader(fileName)
    val lexer: RecognizeLexeme = RecognizeLexeme(file)
    lexer.tokens match {
      case Backslash(span6) :: Identifier("Hans_Georg", span5) :: Colon(span4) :: Type(FloatTyp(), span3) :: Arrow(span2) :: Identifier("Hans_Georg", span1) :: Nil => true
      case a => throw new Exception(a.toString())
    }
  }

  "RecognizeLexeme" should "work for TypWith-" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/TypWith-.rise"
    val file: FileReader =  FileReader(fileName)
    val thrown = intercept[Exception] {
      RecognizeLexeme(file)
    }
    val expected: String = "ErrorToken: It is an '->' expected. The Lexeme '--' is not an '->'! at FileReader: fileName: 'src/test/scala/parser/readFiles/filesToLexe/TypWith-.rise'; fileContent: {\n\\x:I32-->x\n}; beginLocation: (column: 0 ; row: 6); endLocation: (column: 0 ; row: 7)\n\\x:I32-̲->x"
    thrown.getMessage should equal(expected)
  }

  "RecognizeLexeme" should "work for noBacklashAtBeginning.rise" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/noBacklashAtBeginning.rise"
    val file: FileReader =  FileReader(fileName)
    val thrown = intercept[Exception] {
      RecognizeLexeme(file)
    } //Todo: should we have a underline of the important code here too? yes, but here is no
    val expected: String = "ErrorToken: It is an '\\' expected. The Lexeme 'x' is not an '\\'! at FileReader: fileName: 'src/test/scala/parser/readFiles/filesToLexe/noBacklashAtBeginning.rise'; fileContent: {\nx:I32->x+5\n}; beginLocation: (column: 0 ; row: 0); endLocation: (column: 0 ; row: 0)\nx:I32->x+5"
    thrown.getMessage should equal(expected)
  }
  "RecognizeLexeme" should "work for plus" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/plus.rise"
    val file: FileReader =  FileReader(fileName)
    val lexer: RecognizeLexeme = RecognizeLexeme(file)
    lexer.tokens match {
      case Backslash(span8) :: Identifier("x", span7) :: Colon(span6) :: Type(IntTyp(), span5) :: Arrow(span4) :: Identifier("x", span3) :: BinOp(BinOpType.ADD, span2) :: I32(5, span1) :: Nil => true
      case a => throw new Exception(a.toString())
    }
  }

  "RecognizeLexeme" should "work for minus" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/minus.rise"
    val file: FileReader =  FileReader(fileName)
    val lexer: RecognizeLexeme = RecognizeLexeme(file)
    lexer.tokens match {
      case Backslash(span8) :: Identifier("x", span7) :: Colon(span6) :: Type(IntTyp(), span5) :: Arrow(span4) :: Identifier("x", span3) :: BinOp(BinOpType.SUB, span2) :: I32(5, span1) :: Nil => true
      case a => throw new Exception(a.toString())
    }
  }

  "RecognizeLexeme" should "work for negation" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/negation.rise"
    val file: FileReader =  FileReader(fileName)
    val lexer: RecognizeLexeme = RecognizeLexeme(file)
    lexer.tokens match {
      case Backslash(span8) :: Identifier("y", span7) :: Colon(span6) :: Type(IntTyp(), span5) :: Arrow(span4) :: UnOp(UnaryOpType.NEG, span2) :: Identifier("y", span1) :: Nil => true
      case a => throw new Exception(a.toString())
    }
  }

  "RecognizeLexeme" should "work for negationWithBool" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/negationWithBool.rise"
    val file: FileReader =  FileReader(fileName)
    val lexer: RecognizeLexeme = RecognizeLexeme(file)
    lexer.tokens match {
      case Backslash(span8) :: Identifier("b", span7) :: Colon(span6) :: Type(BoolType(), span5) :: Arrow(span4) :: UnOp(UnaryOpType.NEG, span2) :: Identifier("b", span1) :: Nil => true
      case a => throw new Exception(a.toString())
    }
  }

  "RecognizeLexeme" should "work for not" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/not.rise"
    val file: FileReader =  FileReader(fileName)
    val lexer: RecognizeLexeme = RecognizeLexeme(file)
    lexer.tokens match {
      case Backslash(span8) :: Identifier("b", span7) :: Colon(span6) :: Type(BoolType(), span5) :: Arrow(span4) :: UnOp(UnaryOpType.NOT, span2) :: Identifier("b", span1) :: Nil => true
      case a => throw new Exception(a.toString())
    }
  }

  "RecognizeLexeme" should "work for braces" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/braces.rise"
    val file: FileReader =  FileReader(fileName)
    val lexer: RecognizeLexeme = RecognizeLexeme(file)
    lexer.tokens match {
      case Backslash(span8) :: Identifier("b", span7) :: Colon(span6) :: Type(BoolType(), span5) :: Arrow(span4) :: LBrace(span3) :: Identifier("b", span2) :: RBrace(span1) :: Nil => true
      case a => throw new Exception(a.toString())
    }
  }

  "RecognizeLexeme" should "work for bracesWithNot" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/bracesWithNot.rise"
    val file: FileReader =  FileReader(fileName)
    val lexer: RecognizeLexeme = RecognizeLexeme(file)
    lexer.tokens match {
      case Backslash(span8) :: Identifier("b", span7) :: Colon(span6) :: Type(BoolType(), span5) :: Arrow(span4) :: LBrace(span3) :: UnOp(UnaryOpType.NOT, span2) :: Identifier("b", span1) :: RBrace(span0) :: Nil => true
      case a => throw new Exception(a.toString())
    }
  }

  "RecognizeLexeme" should "work for LeftBraceMissing.rise" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/LeftBraceMissing.rise"
    val file: FileReader =  FileReader(fileName)
    val thrown = intercept[Exception] {
      RecognizeLexeme(file)
    } //Todo: should we have a underline of the important code here too? yes, but here is no
    val expected: String = "ErrorToken: Left Brace is missing! at FileReader: fileName: 'src/test/scala/parser/readFiles/filesToLexe/LeftBraceMissing.rise'; fileContent: {\n\\b:bool-> b )\n}; beginLocation: (column: 0 ; row: 12); endLocation: (column: 0 ; row: 12)\n\\b:bool-> b )"
    thrown.getMessage should equal(expected)
  }

  "RecognizeLexeme" should "work for RightBraceMissing.rise" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/RightBraceMissing.rise"
    val file: FileReader =  FileReader(fileName)
    val thrown = intercept[Exception] {
      RecognizeLexeme(file)
    }
    val expected: String = "ErrorToken: Right Brace is missing! at FileReader: fileName: 'src/test/scala/parser/readFiles/filesToLexe/RightBraceMissing.rise'; fileContent: {\n\\b:bool->(   b\n}; beginLocation: (column: 0 ; row: 13); endLocation: (column: 0 ; row: 14)\n\\b:bool->(   b̲"
    thrown.getMessage should equal(expected)
  }

  "RecognizeLexeme" should "work for tooMuchRightBraces.rise" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/tooMuchRightBraces.rise"
    val file: FileReader =  FileReader(fileName)
    val thrown = intercept[Exception] {
      RecognizeLexeme(file)
    } //Todo: should we have a underline of the important code here too? yes, but here is no
    val expected: String = "ErrorToken: Left Brace is missing! at FileReader: fileName: 'src/test/scala/parser/readFiles/filesToLexe/tooMuchRightBraces.rise'; fileContent: {\n\\b:bool->( b )      )\n}; beginLocation: (column: 0 ; row: 20); endLocation: (column: 0 ; row: 20)\n\\b:bool->( b )      )"
    thrown.getMessage should equal(expected)
  }

  "RecognizeLexeme" should "work for tooMuchLeftBraces.rise" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/tooMuchLeftBraces.rise"
    val file: FileReader =  FileReader(fileName)
    val thrown = intercept[Exception] {
      RecognizeLexeme(file)
    }
    val expected: String = "ErrorToken: Right Brace is missing! at FileReader: fileName: 'src/test/scala/parser/readFiles/filesToLexe/tooMuchLeftBraces.rise'; fileContent: {\n\\b:bool->(( b )\n}; beginLocation: (column: 0 ; row: 15); endLocation: (column: 0 ; row: 15)\n\\b:bool->(( b )"
    thrown.getMessage should equal(expected)
  }

  "RecognizeLexeme" should "complexInOneLine" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/complexInOneLine.rise"
    val file: FileReader =  FileReader(fileName)
    val lexer: RecognizeLexeme = RecognizeLexeme(file)
    lexer.tokens match { //\x:I32->\y->-(x*y)+42%5
      case Backslash(_) :: Identifier("x", _) :: Colon(_) :: Type(IntTyp(), _) :: Arrow(_) :: Backslash(_) :: Identifier("y", _) :: Arrow(_) :: UnOp(UnaryOpType.NEG, _) :: LBrace(_) :: Identifier("x", _) :: BinOp(BinOpType.MUL, _) :: Identifier("y", _) :: RBrace(_) :: BinOp(BinOpType.ADD, _) :: I32(42, _) :: BinOp(BinOpType.MOD, _) :: I32(5, _) :: Nil => true
      case a => throw new Exception(a.toString())
    }
  }

  "RecognizeLexeme" should "complexInThreeLines" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/complexInThreeLines.rise"
    val file: FileReader =  FileReader(fileName)
    val lexer: RecognizeLexeme = RecognizeLexeme(file)
    lexer.tokens match {
      case Backslash(_) :: Identifier("x", _) :: Colon(_) :: Type(IntTyp(), _) :: Arrow(_) :: Backslash(_) :: Identifier("y", _) :: Colon(_) :: Type(IntTyp(), _) :: Arrow(_) :: UnOp(UnaryOpType.NEG, _) :: LBrace(_) :: Identifier("x", _) :: BinOp(BinOpType.MUL, _) :: Identifier("y", _) :: RBrace(_) :: BinOp(BinOpType.ADD, _) :: I32(42, _) :: BinOp(BinOpType.MOD, _) :: I32(5, _) :: Nil => true
      case a => throw new Exception(a.toString())
    }
  }

  "RecognizeLexeme" should "veryComplicated.rise" in {
    val fileName: String = "src/test/scala/parser/readFiles/filesToLexe/veryComplicated.rise"
    val file: FileReader =  FileReader(fileName)
    val lexer: RecognizeLexeme = RecognizeLexeme(file)
    lexer.tokens match {
      case Backslash(_) :: Identifier("Michael", _) :: Colon(_) :: Type(BoolType(), _) :: Arrow(_) :: Backslash(_) :: Identifier("Heinrich", _) :: Colon(_) :: Type(BoolType(), _) :: Arrow(_) :: UnOp(UnaryOpType.NOT, _) :: LBrace(_) :: LBrace(_) :: Backslash(_) :: Identifier("varX", _) :: Colon(_) :: Type(IntTyp(), _) :: Arrow(_) :: Backslash(_) :: Identifier("varY", _) :: Colon(_) :: Type(FloatTyp(), _) :: Arrow(_) :: Identifier("varX", _) :: BinOp(BinOpType.MUL, _) :: Identifier("varY", _) :: BinOp(BinOpType.MUL, _) :: LBrace(_) :: I32(25, _) :: BinOp(BinOpType.SUB, _) :: F32(a, _) :: RBrace(_) :: BinOp(BinOpType.DIV, _) :: F32(b, _) :: RBrace(_) :: BinOp(BinOpType.MOD, _) :: I32(42, _) :: BinOp(BinOpType.EQ, _) :: I32(0, _) :: RBrace(_) :: Nil => {
        a == 10.5 && b == 2.3 //I can't write 2.3 directly in the pattern match, because then it would be unequal
      }
      case a => throw new Exception(a.toString())
    }
  }
}