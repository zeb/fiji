#include <stdio.h>
#include <time.h>
#include <stdlib.h>

void quicksort(double* values, int* indices,
	       const int left, const int right, const int pivot) {
	int i = left;
	int j = right;
	const double x = values[pivot];
	do {
		while (values[i] < x) i++;
		while (x < values[j]) j--;
		if (i <= j) {
			// Swap
			const double tmpD = values[i];
			values[i] = values[j];
			values[j] = tmpD;
			const int tmpI = indices[i];
			indices[i] = indices[j];
			indices[j] = tmpI;
			//
			i++;
			j--;
		}
	} while (i <= j);
	if (left < j) quicksort(values, indices, left, j, (left + j) / 2);
	if (i < right) quicksort(values, indices, i, right, (i + right) / 2);
}


int main(int argc, char *argv[]) {

	/* Test: */
	const int size = 1000000;
	int* indices = malloc(size * sizeof(int));
	double* values = malloc(size * sizeof(double));
	int i=0;
	for (;i<size; i++) {
		indices[i] = i;
		values[i] = drand48();
		/*printf("%i : %f\n", indices[i], values[i]);*/
	}

	const clock_t start = clock();

	quicksort(values, indices, 0, size-1, size/2);

	const clock_t end = clock();

	printf("start: %f\n", (double)start);
	printf("end: %f\n", (double)end);
	printf("clocks per sec: %li\n", CLOCKS_PER_SEC);
	printf("Elapsed time: %f seconds\n", ((double)end - start) / CLOCKS_PER_SEC);

	/*
	for (i=0; i<size; i++) {
		printf("%i : %f\n", indices[i], values[i]);
	}
	*/

	free(indices);
	free(values);

	return 0;
}

/* Compile with: gcc -Wall -O4 -o sort.o sort.c
 * Result: takes 150 ms for 10^6 elements, vs 180 ms for the java version. */
