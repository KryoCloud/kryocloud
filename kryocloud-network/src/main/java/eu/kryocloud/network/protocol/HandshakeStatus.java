package eu.kryocloud.network.protocol;

public enum HandshakeStatus {

    ACCEPTED,
    UNSUPPORTED_PROTOCOL,
    INVALID_TOKEN,
    INVALID_IDENTITY,
    INVALID_PEER_TYPE
}
