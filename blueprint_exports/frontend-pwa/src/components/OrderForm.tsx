import React, { useState } from 'react';

export interface OrderItem {
  id: string;
  darijaTitle: string;
  priceMad: number; // Exact Temu regular price in MAD (0% markup)
  quantity: number;
  imageUrl: string;
}

interface OrderFormProps {
  items: OrderItem[];
  whatsappBusinessNumber?: string; // e.g. "212661000000"
  onOrderCompleted?: () => void;
}

const FIXED_SERVICE_FEE_MAD = 15;
const MOROCCAN_PHONE_REGEX = /^(?:(?:\+|00)212|0)[5-7]\d{8}$/;

export const OrderForm: React.FC<OrderFormProps> = ({
  items,
  whatsappBusinessNumber = '212661000000',
  onOrderCompleted,
}) => {
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submittedOrderCode, setSubmittedOrderCode] = useState<string | null>(null);

  const subtotalMad = items.reduce((sum, item) => sum + item.priceMad * item.quantity, 0);
  const finalTotalMad = subtotalMad + FIXED_SERVICE_FEE_MAD;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (submittedOrderCode) return; // Duplicate submission guard

    const cleanPhone = phone.replace(/[\s-]/g, '');
    if (fullName.trim().length < 2) {
      setError('المرجو إدخال الاسم الكامل');
      return;
    }
    if (!MOROCCAN_PHONE_REGEX.test(cleanPhone)) {
      setError('رقم الهاتف المغربي غير صحيح (مثال: 0661234567)');
      return;
    }
    if (address.trim().length < 4) {
      setError('المرجو تحديد العنوان أو الحي فالجديدة');
      return;
    }

    setError(null);
    const orderCode = `JD-${Math.floor(1000 + Math.random() * 9000)}`;

    const darijaSummary = [
      `السلام عليكم! بغيت نأكد هاد الطلبية فالجديدة:`,
      `🔖 رقم الطلب: ${orderCode}`,
      `👤 الاسم: ${fullName.trim()}`,
      `📱 الهاتف: ${cleanPhone}`,
      `📍 العنوان: ${address.trim()}`,
      `📦 السلع:`,
      ...items.map((i, idx) => `${idx + 1}. ${i.darijaTitle} (x${i.quantity})`),
      `💰 المجموع النهائي الواجب أداؤه: ${finalTotalMad} درهم`,
    ].join('\n');

    // Lock local UI into confirmation state BEFORE redirecting to WhatsApp
    setSubmittedOrderCode(orderCode);
    onOrderCompleted?.();

    const waUrl = `https://wa.me/${whatsappBusinessNumber}?text=${encodeURIComponent(darijaSummary)}`;
    window.location.href = waUrl;
  };

  if (submittedOrderCode) {
    return (
      <div dir="rtl" className="p-6 rounded-2xl bg-emerald-950 text-white text-center space-y-4">
        <div className="text-3xl font-bold text-amber-400">✓ تم تسجيل طلبك بنجاح</div>
        <p className="text-base">
          رقم الطلبية: <strong>{submittedOrderCode}</strong>
        </p>
        <p className="text-sm text-emerald-200">
          تم تحويلك إلى واتساب لتأكيد الطلبية. لا داعي لإعادة إرسال الاستمارة.
        </p>
      </div>
    );
  }

  return (
    <form dir="rtl" onSubmit={handleSubmit} className="space-y-4 max-w-lg mx-auto">
      <div>
        <label className="block font-bold mb-1">الاسم الكامل</label>
        <input
          type="text"
          required
          value={fullName}
          onChange={(e) => setFullName(e.target.value)}
          placeholder="مثال: أمين الجديدي"
          className="w-full rounded-xl border border-stone-300 px-4 py-3"
        />
      </div>

      <div>
        <label className="block font-bold mb-1">رقم الهاتف (06 أو 07)</label>
        <input
          type="tel"
          required
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          placeholder="0661234567"
          className="w-full rounded-xl border border-stone-300 px-4 py-3"
        />
      </div>

      <div>
        <label className="block font-bold mb-1">العنوان فالجديدة</label>
        <input
          type="text"
          required
          value={address}
          onChange={(e) => setAddress(e.target.value)}
          placeholder="سيدي بوزيد، حي المطار، القلعة..."
          className="w-full rounded-xl border border-stone-300 px-4 py-3"
        />
      </div>

      {error && <div className="p-3 rounded-xl bg-red-100 text-red-800 text-sm font-bold">{error}</div>}

      <div className="p-4 rounded-2xl bg-[#0F2922] text-white flex items-center justify-between">
        <span className="text-sm">المجموع النهائي (شامل خدمة التوصيل):</span>
        <span className="text-2xl font-extrabold text-[#FFB300]">{finalTotalMad} درهم</span>
      </div>

      <button
        type="submit"
        className="w-full py-4 rounded-2xl bg-[#1EBE57] hover:bg-[#19a34a] text-white font-extrabold text-lg shadow-lg"
      >
        أكد الطلب عبر واتساب
      </button>
    </form>
  );
};
