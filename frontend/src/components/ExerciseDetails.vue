<script setup>
import { ref, watch } from 'vue';

const props = defineProps({
  exercise: {
    type: Object,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['save']);
const result = ref('');

watch(
  () => props.exercise,
  (exercise) => {
    result.value = exercise?.result ?? '';
  },
  { immediate: true }
);

const handleSubmit = (event) => {
  event.preventDefault();
  emit('save', props.exercise.rowNumber, result.value);
};
</script>

<template>
  <div class="details">
    <h2>{{ exercise.name || 'Exercise details' }}</h2>
    <p class="meta">
      <strong>Reps:</strong> {{ exercise.repetitions || '—' }} |<strong> Weight:</strong> {{ exercise.weight || '—' }}
    </p>
    <p class="comment" v-if="exercise.comment">{{ exercise.comment }}</p>

    <form @submit="handleSubmit">
      <label for="result">Result (column 7)</label>
      <input id="result" v-model="result" placeholder="e.g. 12 reps" />

      <button type="submit" :disabled="loading">
        {{ loading ? 'Saving…' : 'Save result' }}
      </button>
    </form>
  </div>
</template>

<style scoped>
.details {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.meta {
  color: #4b5563;
}

.comment {
  background: #f1f5f9;
  border-radius: 8px;
  padding: 0.75rem;
}

form {
  margin-top: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

label {
  font-weight: 600;
}

input {
  padding: 0.75rem 1rem;
  border-radius: 10px;
  border: 1px solid #cbd5f5;
}

button {
  padding: 0.75rem 1rem;
  background: #0ea5e9;
  border: none;
  border-radius: 10px;
  color: white;
  font-weight: 600;
  cursor: pointer;
}

button[disabled] {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>
