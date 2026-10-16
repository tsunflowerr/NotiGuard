package vn.notiguard;

import android.net.Uri;
import android.os.*;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Watches only selected Settings URIs using the shell process's ContentService Binder.
 * Context ContentResolver is unsuitable for a standalone Shizuku UserService.
 * Protocol checked against AOSP android16-release IContentService/IContentObserver.
 */
final class SettingsObserver implements AutoCloseable {
    private static final String DESCRIPTOR = "android.database.IContentObserver";
    private final Set<Uri> uris;
    private final Runnable changed;
    private final Runnable disconnected;
    private final Object contentService;
    private final Object transport;
    private final Method unregister;
    private final IBinder serviceBinder;
    private final IBinder.DeathRecipient deathRecipient;
    private volatile boolean closed;

    private final Binder observer = new Binder() {
        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == INTERFACE_TRANSACTION) { if (reply != null) reply.writeString(DESCRIPTOR); return true; }
            if (code != FIRST_CALL_TRANSACTION && code != FIRST_CALL_TRANSACTION + 1) return super.onTransact(code, data, reply, flags);
            // ContentService delivers these callbacks from system_server; no public trigger endpoint.
            if (Binder.getCallingUid() != 1000) return false;
            data.enforceInterface(DESCRIPTOR);
            data.readInt(); // selfChange
            Uri[] delivered;
            int user;
            if (code == FIRST_CALL_TRANSACTION) {
                Uri uri = data.readInt() == 0 ? null : Uri.CREATOR.createFromParcel(data);
                delivered = new Uri[] { uri }; user = data.readInt();
            } else {
                delivered = data.createTypedArray(Uri.CREATOR); data.readInt(); user = data.readInt();
            }
            if (data.dataAvail() != 0) throw new BadParcelableException("Unexpected observer payload");
            if (!closed && (user == 0 || user == -1) && delivered != null) {
                for (Uri uri : delivered) if (uris.contains(uri)) { changed.run(); break; }
            }
            return true;
        }
    };

    SettingsObserver(Set<Uri> requested, Runnable changed, Runnable disconnected) throws Exception {
        if (requested.isEmpty()) throw new IllegalArgumentException("No supported setting to observe");
        this.uris = Set.copyOf(requested); this.changed = changed; this.disconnected = disconnected;
        Class<?> serviceManager = Class.forName("android.os.ServiceManager");
        serviceBinder = (IBinder) serviceManager.getMethod("getService", String.class).invoke(null, "content");
        if (serviceBinder == null) throw new IllegalStateException("ContentService unavailable");
        contentService = Class.forName("android.content.IContentService$Stub").getMethod("asInterface", IBinder.class).invoke(null, serviceBinder);
        Class<?> observerType = Class.forName(DESCRIPTOR);
        transport = Class.forName(DESCRIPTOR + "$Stub").getMethod("asInterface", IBinder.class).invoke(null, observer);
        Class<?> serviceType = Class.forName("android.content.IContentService");
        unregister = serviceType.getMethod("unregisterContentObserver", observerType);
        Method register = serviceType.getMethod("registerContentObserver", Uri.class, boolean.class, observerType, int.class, int.class);
        deathRecipient = () -> { if (!closed) this.disconnected.run(); };
        boolean linked = false;
        try {
            serviceBinder.linkToDeath(deathRecipient, 0); linked = true;
            for (Uri uri : uris) register.invoke(contentService, uri, false, transport, 0, Build.VERSION.SDK_INT);
        } catch (Exception e) {
            closed = true;
            if (linked) serviceBinder.unlinkToDeath(deathRecipient, 0);
            try { unregister.invoke(contentService, transport); } catch (Exception ignored) { /* Best effort after partial registration. */ }
            throw e;
        }
    }

    @Override public void close() throws Exception {
        closed = true;
        try { unregister.invoke(contentService, transport); }
        finally { serviceBinder.unlinkToDeath(deathRecipient, 0); }
    }
}
