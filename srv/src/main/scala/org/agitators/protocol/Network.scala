package org.agitators.protocol

final object Network {

  case class Frontend(name: String, version: String)

  case class Client(id: Int, frontend: Frontend)


  case class Connect(client: Client)

  case class Disconnect(client: Client)

  case class Authenticate() { ??? }


  case class Shutdown()
}
