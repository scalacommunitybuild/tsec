package tsec.cipher.symmetric.libsodium

import cats.effect.IO
import org.scalatest.MustMatchers
import org.scalatest.prop.PropertyChecks
import tsec.{ScalaSodium, TestSpec}
import tsec.cipher.symmetric._
import tsec.cipher.symmetric.PlainText
import tsec.cipher.symmetric.libsodium.internal.SodiumCipherPlatform
import tsec.common._

class SodiumCipherTest extends SodiumSpec {

  final def testSecretBoxCipher[A](platform: SodiumCipherPlatform[A]) = {
    behavior of s"${platform.algorithm} symmetric key"

    it should "generate key, encrypt and decrypt properly" in {
      forAll { (s: String) =>
        val pt = PlainText(s.utf8Bytes)
        val program = for {
          key     <- platform.generateKey[IO]
          encrypt <- platform.encrypt[IO](pt, key)
          decrypt <- platform.decrypt[IO](encrypt, key)
        } yield decrypt
        val attempted = program.attempt.unsafeRunSync()
        if (attempted.map(_.content.toHexString) != Right(pt.content.toHexString)) {
          println(attempted.map(_.content.toHexString))
          println(pt.content.toHexString)
        }

        if (!s.isEmpty)
          program.unsafeRunSync().content.toHexString mustBe pt.content.toHexString
      }
    }

    it should "not decrypt properly for a wrong key" in {
      forAll { (s: String) =>
        val pt = PlainText(s.utf8Bytes)
        if (!s.isEmpty)
          (for {
            key     <- platform.generateKey[IO]
            key2    <- platform.generateKey[IO]
            encrypt <- platform.encrypt[IO](pt, key)
            decrypt <- platform.decrypt[IO](encrypt, key2)
          } yield decrypt).attempt.unsafeRunSync() mustBe a[Left[CipherError, _]]
      }
    }

    it should "encrypt and decrypt properly with a split tag" in {
      forAll { (s: String) =>
        val pt = PlainText(s.utf8Bytes)
        if (!s.isEmpty)
          (for {
            key           <- platform.generateKey[IO]
            encryptedPair <- platform.encryptDetached[IO](pt, key)
            decrypt       <- platform.decryptDetached[IO](encryptedPair._1, key, encryptedPair._2)
          } yield decrypt).unsafeRunSync().content.toHexString mustBe pt.content.toHexString
      }
    }

    it should "not decrypt properly with an incorrect key detached" in {
      forAll { (s: String) =>
        val pt = PlainText(s.utf8Bytes)
        if (!s.isEmpty)
          (for {
            key     <- platform.generateKey[IO]
            key2    <- platform.generateKey[IO]
            encrypt <- platform.encryptDetached[IO](pt, key)
            decrypt <- platform.decryptDetached[IO](encrypt._1, key2, encrypt._2)
          } yield decrypt).attempt.unsafeRunSync() mustBe a[Left[CipherError, _]]
      }
    }

    it should "not decrypt properly with an incorrect tag but correct key" in {
      forAll { (s: String) =>
        val pt = PlainText(s.utf8Bytes)
        if (!s.isEmpty)
          (for {
            key         <- platform.generateKey[IO]
            encrypt     <- platform.encryptDetached[IO](pt, key)
            randomBytes <- ScalaSodium.randomBytes[IO](platform.macLen)
            decrypt     <- platform.decryptDetached[IO](encrypt._1, key, AuthTag.is[A].coerce(randomBytes))
          } yield decrypt).attempt.unsafeRunSync() mustBe a[Left[CipherError, _]]
      }
    }
  }

  testSecretBoxCipher(XSalsa20Poly1305)
  testSecretBoxCipher(XChacha20Poly1305)

}
