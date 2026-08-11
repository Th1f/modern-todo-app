import { request } from "./client";

export interface Category {
  id: number;
  name: string;
  color: string;
}

export type NewCategoryInput = Omit<Category, "id">;

export const getCategories = () => request<Category[]>("/categories");

export const createCategory = (category: NewCategoryInput) =>
  request<Category>("/categories", {
    method: "POST",
    body: JSON.stringify(category),
  });

export interface CategoryPatch {
  name?: string;
  color?: string;
}

export const updateCategory = (id: number, patch: CategoryPatch) =>
  request<Category>(`/categories/${id}`, {
    method: "PATCH",
    body: JSON.stringify(patch),
  });

export const deleteCategory = (id: number) =>
  request<void>(`/categories/${id}`, { method: "DELETE" });
