package cloud.apposs.netkit.server.annotation;

import cloud.apposs.netkit.server.IoServer;

import java.lang.annotation.Annotation;

public interface AnnotationParser {
	Class<? extends Annotation> getAnnotationType();
	
	void parse(IoServer server, Annotation annotation);
}
