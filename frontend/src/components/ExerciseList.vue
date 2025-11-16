<script setup>
const props = defineProps({
  exercises: {
    type: Array,
    required: true
  },
  selectedRowNumber: {
    type: Number,
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['select']);

const handleSelect = (exercise) => {
  emit('select', exercise);
};
</script>

<template>
  <div class="list">
    <h2>Exercises</h2>
    <p v-if="loading && !exercises.length">Loading…</p>
    <ul v-else>
      <li
        v-for="exercise in exercises"
        :key="exercise.rowNumber"
        :class="{ active: exercise.rowNumber === selectedRowNumber }"
      >
        <button type="button" @click="handleSelect(exercise)">
          <span class="name">{{ exercise.name || 'Unnamed exercise' }}</span>
          <span class="meta">{{ exercise.repetitions }} reps · {{ exercise.weight }} kg</span>
        </button>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.list {
  background: white;
  border-radius: 12px;
  padding: 1rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  max-height: 70vh;
  overflow-y: auto;
}

ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

li + li {
  margin-top: 0.5rem;
}

button {
  width: 100%;
  text-align: left;
  background: transparent;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 0.75rem;
  cursor: pointer;
}

button:hover {
  border-color: #0ea5e9;
}

li.active button {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 2px rgba(14, 165, 233, 0.2);
}

.name {
  display: block;
  font-weight: 600;
}

.meta {
  color: #6b7280;
  font-size: 0.9rem;
}
</style>
