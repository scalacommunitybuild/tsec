package tsec.jws

import tsec.jws.signature.JWSSignature
import tsec.jws.header.{JWSHeader, JWSSignedHeader}
import tsec.jwt.claims.JWTClaims
import tsec.mac.instance.MacTag

trait JWSJWT[A] {
  def header: JWSHeader[A]

  def body: JWTClaims

  def signature: JWSSignature[A]
}
