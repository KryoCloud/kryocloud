package eu.kryocloud.api.plugin.internal.protocol;

public enum PluginWireMessageType {

    HANDSHAKE,
    REQUEST,
    RESPONSE,
    EVENT,
    SUBSCRIBE,
    UNSUBSCRIBE,
    MESSAGE,
    ACK,
    ERROR

}
