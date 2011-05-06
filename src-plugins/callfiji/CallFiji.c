#include <CallFiji.h>
#include <limits.h>
#include <string.h>

#ifndef WIN32
#define __USE_GNU
#include <dlfcn.h>
#endif

const char *fiji_path(void)
{
#ifdef WIN32
#else
	static char path[PATH_MAX];
	Dl_info info;
	int i, end;

	/* static char arrays are initialized to NUL */
	if (path[0])
		return path;

	if (!dladdr(fiji_path, &info))
		return NULL;

	/* strip lib/<platform>/<libname> */
	end = strlen(info.dli_fname);
	for (i = 0; i < 3; i++)
		while (--end >= 0 && info.dli_fname[end] != '/')
			; /* ignore */
	if (!end || end > sizeof(path))
		return NULL;
	strncpy(path, info.dli_fname, end);
	return path;
#endif
}

JNIEXPORT void JNICALL Java_CallFiji_run(JNIEnv *env, jclass clazz)
{
	fprintf(stderr, "Hello\n");
}