import React from 'react';

export interface ReceiptOrderItem {
  darijaTitle: string; // White-labeled Darija title ONLY (no supplier/Temu titles)
  priceMad: number;
  quantity: number;
  imageUrl: string;
}

export interface ReceiptGeneratorProps {
  orderCode: string;
  customerName: string;
  customerPhone: string;
  customerAddress: string;
  items: ReceiptOrderItem[];
  serviceFeeMad?: number;
}

/**
 * White-Labeled Single-Page A4 Receipt Generator Component.
 * - Excludes ALL original supplier branding/logos/titles (Zero Temu references).
 * - Dynamically scales and distributes product images across 1, 2, or 3 neat cards on a single A4 page.
 */
export const ReceiptGenerator: React.FC<ReceiptGeneratorProps> = ({
  orderCode,
  customerName,
  customerPhone,
  customerAddress,
  items,
  serviceFeeMad = 15,
}) => {
  const subtotalMad = items.reduce((acc, item) => acc + item.priceMad * item.quantity, 0);
  const totalMad = subtotalMad + serviceFeeMad;
  const featuredCards = items.slice(0, 3);
  const gridColsClass =
    featuredCards.length === 1
      ? 'grid-cols-1 max-w-xs mx-auto'
      : featuredCards.length === 2
      ? 'grid-cols-2'
      : 'grid-cols-3';

  return (
    <div dir="rtl" className="space-y-4">
      <div className="flex justify-end print:hidden">
        <button
          type="button"
          onClick={() => window.print()}
          className="px-5 py-2.5 rounded-xl bg-[#E65100] text-white font-bold shadow"
        >
          طباعة / تحميل توصيل A4 PDF
        </button>
      </div>

      <section
        className="mx-auto bg-white text-[#141D1A] border border-stone-300 rounded-2xl p-6 flex flex-col justify-between"
        style={{
          width: '210mm',
          height: '297mm',
          maxHeight: '297mm',
          boxSizing: 'border-box',
          pageBreakAfter: 'avoid',
          pageBreakInside: 'avoid',
          fontFamily: "'Cairo', 'Tajawal', sans-serif",
        }}
      >
        {/* White-Label Local Header */}
        <header className="bg-[#0F2922] text-white p-5 rounded-xl flex justify-between items-center">
          <div>
            <h1 className="text-xl font-extrabold">مازاغان إكسبريس • الجديدة</h1>
            <p className="text-xs text-amber-400">توصيل تسليم محلي معتمد (White-Label Delivery Receipt)</p>
          </div>
          <div className="text-left text-xs">
            <div className="font-bold text-sm">رقم التوصيل: {orderCode}</div>
            <div>التاريخ: {new Date().toLocaleDateString('ar-MA')}</div>
          </div>
        </header>

        {/* Customer Info */}
        <div className="grid grid-cols-3 gap-3 bg-stone-50 p-4 rounded-xl border border-stone-200 text-sm">
          <div><strong>الزبون:</strong> {customerName}</div>
          <div><strong>الهاتف:</strong> {customerPhone}</div>
          <div><strong>العنوان:</strong> {customerAddress}</div>
        </div>

        {/* Dynamic 1 / 2 / 3 Product Photo Cards fitted onto the same A4 page */}
        <div className={`grid gap-3 ${gridColsClass}`}>
          {featuredCards.map((item, idx) => (
            <div
              key={idx}
              className="border border-stone-200 rounded-xl p-2.5 flex flex-col items-center bg-stone-50"
            >
              <img
                src={item.imageUrl}
                alt={item.darijaTitle}
                className={featuredCards.length === 1 ? 'h-44 object-contain' : 'h-32 object-contain'}
              />
              <span className="mt-2 text-xs font-bold text-center line-clamp-1">
                {item.darijaTitle}
              </span>
            </div>
          ))}
        </div>

        {/* Line Items Table */}
        <table className="w-full text-sm border-collapse">
          <thead>
            <tr className="border-b-2 border-stone-300 text-right">
              <th className="py-2">المنتج (بالدارجة)</th>
              <th className="py-2">الكمية</th>
              <th className="py-2 text-left">الثمن (MAD)</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item, idx) => (
              <tr key={idx} className="border-b border-stone-200">
                <td className="py-2 font-medium">{item.darijaTitle}</td>
                <td className="py-2">x{item.quantity}</td>
                <td className="py-2 text-left font-bold">{item.priceMad * item.quantity} درهم</td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* Single-Page Totals Footer */}
        <footer className="bg-stone-100 p-4 rounded-xl flex justify-between items-center border border-stone-300">
          <div className="text-xs text-stone-600">
            <div>مجموع السلع الأصلي: {subtotalMad} درهم</div>
            <div>خدمة التوصيل المحلي بالجديدة: +{serviceFeeMad} درهم</div>
          </div>
          <div className="text-xl font-extrabold text-[#E65100]">
            المجموع النهائي: {totalMad} درهم
          </div>
        </footer>
      </section>
    </div>
  );
};
