import api from '@/api/index'

export interface CounselorApplicationItem {
  id: number
  userId: number
  realName: string
  phone: string
  qualificationType: string
  status: 'pending' | 'approved' | 'rejected'
  rejectedReason?: string
  createdAt: string
  updatedAt: string
}

export async function fetchApplications(params?: { status?: string; query?: string }) {
  return await api.get<CounselorApplicationItem[]>('/admin/applications', { params })
}

export async function approveApplication(id: number) {
  const ok = await api.post<boolean>(`/admin/applications/${id}/approve`)
  return ok
}

export async function rejectApplication(id: number, reason: string) {
  const ok = await api.post<boolean>(`/admin/applications/${id}/reject`, { reason })
  return ok
}

export interface CounselorProfile {
  userId: number
  username: string
  title: string
  defaultMode: string
  bio?: string
  tags?: string[]
}

export async function fetchCounselorProfile(username: string) {
  return await api.get<CounselorProfile>(`/counselor/profile/${username}`)
}
