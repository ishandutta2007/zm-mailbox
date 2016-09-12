package com.zimbra.cs.ephemeral;

import java.util.HashMap;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.util.Zimbra;

/**
 * Abstract class representing ephemeral storage.
 *
 * @author iraykin
 *
 */
public abstract class EphemeralStore {

    private static Map<String, String> factories = new HashMap<String, String>();
    private static Factory factory;
    protected AttributeEncoder encoder;

    /**
     * Get the value for a key. If key does not exist, returns an empty
     * EphemeralResult instance.
     *
     * @param key
     * @param location
     * @return
     * @throws ServiceException
     */
    public abstract EphemeralResult get(String key, EphemeralLocation location)
            throws ServiceException;

    /**
     * Set a value for a key if the key does not exist, or overwrites
     * otherwise. If the key is multi-valued, all original values are deleted.
     *
     * @param input
     * @param location
     * @throws ServiceException
     */
    public abstract void set(EphemeralInput attribute, EphemeralLocation location)
            throws ServiceException;

    /**
     * Set a value for a key if the key does not exist, or add another value
     * otherwise. If this value already exists, the expiration is updated if one
     * is provided.
     *
     * @param input
     * @param location
     * @throws ServiceException
     */
    public abstract void update(EphemeralInput attribute, EphemeralLocation location)
            throws ServiceException;

    /**
     * Delete specified value for a key. If the value does not exist, do
     * nothing.
     *
     * @param key
     * @param value
     * @param location
     * @throws ServiceException
     */
    public abstract void delete(String key, String value, EphemeralLocation location)
            throws ServiceException;


    /**
     * Check whether the specified key/value pair exists in the target location.
     *
     * @param key
     * @param value
     * @param location
     * @return
     * @throws ServiceException
     */
    public abstract boolean has(String key, String value, EphemeralLocation location)
            throws ServiceException;

    /**
     * Delete keys that have passed their expiration. If the backend natively
     * supports key expiry, this may do nothing.
     *
     * @param key
     * @param location
     * @throws ServiceException
     */
    public abstract void purgeExpired(String key, EphemeralLocation location)
            throws ServiceException;

    public static void registerFactory(String prefix, String klass) {
        if (factories.containsKey(prefix)) {
            ZimbraLog.ephemeral.warn("Replacing ephemeral factory class '%s' registered for '%s' with '%s'",
                    factories.get(prefix), prefix, klass);
        }
        factories.put(prefix,  klass);
    }

    private static final void setFactory(String factoryClassName) {
        if (factoryClassName == null) {
            Zimbra.halt("no EphemeralStore specified");
        }
        Class<? extends Factory> factoryClass = null;
        try {
            factoryClass = Class.forName(factoryClassName).asSubclass(Factory.class);
        } catch (ClassNotFoundException cnfe) {
            try {
                factoryClass = ExtensionUtil.findClass(factoryClassName).asSubclass(Factory.class);
            } catch (ClassNotFoundException cnfe2) {
                Zimbra.halt("Unable to find EphemeralStore factory " + factoryClassName, cnfe2);
            }
        }
        setFactory(factoryClass);
    }

    @VisibleForTesting
    public static final void setFactory(Class<? extends Factory> factoryClass) {
        try {
            factory = factoryClass.newInstance();
            factory.startup();
            ZimbraLog.ephemeral.info("using ephemeral store factory %s", factoryClass.getDeclaringClass().getSimpleName());
        } catch (InstantiationException | IllegalAccessException e) {
            Zimbra.halt("Unable to initialize EphemeralStore factory " + factoryClass.getDeclaringClass().getSimpleName(), e);
        }
    }

    public void setAttributeEncoder(AttributeEncoder encoder) {
        this.encoder = encoder;
    }

    public AttributeEncoder getAttributeEncoder() {
        return encoder;
    }

    public static Factory getFactory() throws ServiceException {
        if (factory == null) {
            String factoryClass = null;
            String url = Provisioning.getInstance().getConfig().getEphemeralBackendURL();
            if (url != null) {
                String[] tokens = url.split(":");
                if (tokens != null && tokens.length > 0) {
                    factoryClass = factories.get(tokens[0]);
                }
            }
            setFactory(factoryClass);
        }
        return factory;
    }

    public static interface Factory {

        EphemeralStore getStore();
        void startup();
        void shutdown();
    }
}
