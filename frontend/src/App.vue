<script setup>
import { onMounted, ref } from 'vue';
import ExerciseList from './components/ExerciseList.vue';
import ExerciseDetails from './components/ExerciseDetails.vue';
import { fetchExercises, updateExerciseResult } from './services/api.js';

const exercises = ref([]);
const selectedExercise = ref(null);
const loading = ref(false);
const error = ref('');

const loadExercises = async () => {
  loading.value = true;
  error.value = '';
  try {
    const data = await fetchExercises();
    exercises.value = data;
    if (!selectedExercise.value && data.length) {
      selectedExercise.value = data[0];
    } else if (selectedExercise.value) {
      const updated = data.find((exercise) => exercise.rowNumber === selectedExercise.value.rowNumber);
      selectedExercise.value = updated ?? null;
    }
  } catch (err) {
    error.value = err?.message ?? 'Failed to load exercises';
  } finally {
    loading.value = false;
  }
};

const handleSelect = (exercise) => {
  selectedExercise.value = exercise;
};

const handleSave = async (rowNumber, result) => {
  loading.value = true;
  error.value = '';
  try {
    await updateExerciseResult(rowNumber, result);
    await loadExercises();
  } catch (err) {
    error.value = err?.message ?? 'Unable to save result';
  } finally {
    loading.value = false;
  }
};

onMounted(loadExercises);
</script>

<template>
  <main class="page">
    <header>
      <h1>Fitness exercises</h1>
      <p class="subtitle">Pull from Google Sheets and save your results in column 7.</p>
    </header>

    <section class="content">
      <ExerciseList
        :exercises="exercises"
        :selected-row-number="selectedExercise?.rowNumber"
        :loading="loading"
        @select="handleSelect"
      />

      <ExerciseDetails
        v-if="selectedExercise"
        :exercise="selectedExercise"
        :loading="loading"
        @save="handleSave"
      />

      <p v-else class="empty">No exercise selected.</p>
    </section>

    <p v-if="error" class="error">{{ error }}</p>
  </main>
</template>

<style scoped>
.page {
  padding: 2rem;
  max-width: 1000px;
  margin: 0 auto;
}

header {
  margin-bottom: 2rem;
}

.subtitle {
  color: #6b7280;
  margin-top: 0.25rem;
}

.content {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 1.5rem;
}

@media (max-width: 768px) {
  .content {
    grid-template-columns: 1fr;
  }
}

.error {
  color: #b91c1c;
  background: #fee2e2;
  padding: 0.75rem 1rem;
  border-radius: 8px;
}

.empty {
  color: #6b7280;
}
</style>
