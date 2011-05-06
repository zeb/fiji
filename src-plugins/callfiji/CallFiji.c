#include <CallFiji.h>
#include <limits.h>
#include <string.h>

#ifndef WIN32
#define __USE_GNU
#include <dlfcn.h>
#else
#include <windows.h>

static char module_path[PATH_MAX];

BOOL WINAPI DllMain(HINSTANCE dll, DWORD reason, LPVOID reserved)
{
	if (reason != DLL_PROCESS_ATTACH)
		return TRUE;
	GetModuleFileNameA(dll, module_path, sizeof(module_path));
	return TRUE;
}

#endif

JNIEXPORT const char *fiji_path(void)
{
	static char buffer[PATH_MAX];
	const char *path;
	int i, end;
	char slash = '/';
#ifndef WIN32
	Dl_info info;
#endif

	/* static char arrays are initialized to NUL */
	if (buffer[0])
		return buffer;

#ifdef WIN32
	slash = '\\';
	path = module_path;
#else
	if (!dladdr(fiji_path, &info))
		return NULL;

	path = info.dli_fname;
#endif

	/* strip lib/<platform>/<libname> */
	end = strlen(path);
	for (i = 0; i < 3; i++)
		while (--end >= 0 && path[end] != slash)
			; /* ignore */
	if (!end || end > sizeof(buffer))
		return NULL;
	strncpy(buffer, path, end);
	return buffer;
}

JNIEXPORT void JNICALL Java_CallFiji_run(JNIEnv *env, jclass clazz)
{
	fprintf(stderr, "Hello\n");
}