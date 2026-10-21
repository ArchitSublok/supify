import { create } from 'zustand'

// Shared across workspace pages; form values stay in their own reducer/context.
export const useTaskStore = create((set) => ({
  tasks: [
    { id: 'identity', title: 'Check company legal details', done: false },
    { id: 'evidence', title: 'Prepare quality certification evidence', done: false },
    { id: 'review', title: 'Review the supplier portfolio', done: false },
  ],
  addTask(title) {
    const trimmed = title.trim()
    if (!trimmed || trimmed.length > 120) return false
    set((state) => ({ tasks: [...state.tasks, { id: crypto.randomUUID(), title: trimmed, done: false }] }))
    return true
  },
  toggleTask(id) { set((state) => ({ tasks: state.tasks.map((task) => task.id === id ? { ...task, done: !task.done } : task) })) },
  removeTask(id) { set((state) => ({ tasks: state.tasks.filter((task) => task.id !== id) })) },
}))
