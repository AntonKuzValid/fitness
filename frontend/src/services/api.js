import axios from 'axios';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api',
  timeout: 10000
});

export const fetchExercises = async () => {
  const { data } = await client.get('/exercises');
  return data;
};

export const updateExerciseResult = async (rowNumber, result) => {
  await client.post(`/exercises/${rowNumber}/result`, { result });
};
