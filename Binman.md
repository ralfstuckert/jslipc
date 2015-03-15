Most of the Jslipc pipes are file based, and a file is a resource which does not automatically vanish if the process using it dies. In order to give developers a hand when it comes to cleaning up, Jslipc provides some functionality to deal with that, represented by the interface `JslipcBinman`:

```
public interface JslipcBinman extends Closeable {

    /**
     * Attempts to clean up any resources on {@link #close()} if they are no longer needed.
     */
    void cleanUpOnClose();
}
```

If `cleanUpOnClose()` is called prior to `close()`, an implementation of this interface should try to release all resource it relies on (e.g. files, buffers, etc.), whether created by itself or passed in.

Currently all Jslipc streams and pipes implement this interface.