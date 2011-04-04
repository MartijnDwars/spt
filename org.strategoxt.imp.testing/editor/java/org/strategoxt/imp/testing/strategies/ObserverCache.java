package org.strategoxt.imp.testing.strategies;

import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.imp.language.Language;
import org.eclipse.imp.language.LanguageRegistry;
import org.spoofax.interpreter.library.IOAgent;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.dynamicloading.BadDescriptorException;
import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
import org.strategoxt.imp.runtime.services.StrategoObserver;
import org.strategoxt.imp.runtime.stratego.EditorIOAgent;

/** 
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class ObserverCache {
	
	private static final ObserverCache instance = new ObserverCache();
	
	// We need to maintain a reference here to each observer as long as the descriptor lives,
	// as there may not be another reference betweens calls to this class.
	private static final Map<Descriptor, StrategoObserver> asyncCache =
		new WeakHashMap<Descriptor, StrategoObserver>();

	private ObserverCache() {}
	
	public static ObserverCache getInstance() {
		return instance;
	}

	public StrategoObserver getObserver(String languageName, String projectPath) throws BadDescriptorException {
		Descriptor descriptor = getDescriptor(languageName);
		
		return getObserver(descriptor, projectPath);
	}

	public Descriptor getDescriptor(String languageName) throws BadDescriptorException {
		Language language = LanguageRegistry.findLanguage(languageName);
		if (language == null) throw new BadDescriptorException("No language known with the name " + languageName);
		Descriptor descriptor = Environment.getDescriptor(language);
		if (descriptor == null) throw new BadDescriptorException("No language known with the name " + languageName);
		return descriptor;
	}

	private synchronized StrategoObserver getObserver(Descriptor descriptor, String projectPath) throws BadDescriptorException {
		StrategoObserver result = asyncCache.get(descriptor);

		if (result == null)
			result = descriptor.createService(StrategoObserver.class, null);
		
		result.getLock().lock();
		try {
			IOAgent ioAgent = result.getRuntime().getIOAgent();
			if (ioAgent instanceof EditorIOAgent) { 
				// Make the console visible to users
				((EditorIOAgent) ioAgent).getDescriptor().setDynamicallyLoaded(true);
			}
			result.configureRuntime(null, projectPath);
			asyncCache.put(descriptor, result);
		} finally {
			result.getLock().unlock();
		}
		return result;
	}
}
