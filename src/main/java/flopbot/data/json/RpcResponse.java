package flopbot.data.json;

public class RpcResponse<T> {
    public T result;
    public RpcError error;
    public String id;
}
